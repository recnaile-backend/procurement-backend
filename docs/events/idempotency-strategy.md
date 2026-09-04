# Idempotency Strategy

## 1. Why

Kafka delivery in this platform is at-least-once (see transactional-outbox.md). Consumer
rebalances, publisher retries, and broker-side retries can all cause the same logical
event to be delivered more than once. Every consumer must therefore be safe to process the
same `eventId` twice with no duplicate business effect.

## 2. Strategy

```text
Kafka Event
    │
    ▼
Extract eventId
    │
    ▼
Check processed_events collection for eventId (unique index)
    │
    ├── Already present → Ignore (log + increment duplicate-events-skipped metric)
    │
    └── Not present → Process business logic
                            │
                            ▼
                    Upsert business state (not blind insert)
                            │
                            ▼
                    Insert eventId into processed_events (same transaction as the
                    business state upsert, where the driver/deployment topology
                    supports multi-document ACID transactions)
```

## 3. `processed_events` Collection

Per consuming service, per topic (or shared per service — implementation detail, but
never shared across services):

```json
{
  "_id": "ObjectId",
  "eventId": "UUID",          // unique index — this IS the idempotency guard
  "eventType": "INDENT_APPROVED",
  "consumerGroup": "po-service.indent-events",
  "processedAt": "timestamp"
}
```

The unique index on `eventId` is what makes a duplicate insert attempt fail fast and
cheaply if the "check" step ever races (two threads/pods processing the same message
during a rebalance edge case) — the losing insert is caught and treated as "already
processed," not as an error.

## 4. Upsert Semantics

Business-state writes triggered by an event use **upsert / conditional-update** semantics
keyed on the domain identifier (e.g., `poId`, `itemId`) rather than blind inserts, so that
even in the rare case where the `processed_events` check is bypassed (e.g., a bug), a
re-applied event converges to the same end state rather than corrupting it (e.g.,
"set available stock to X" rather than "increment available stock by delta" where delta
could be double-applied — increment-style updates for stock movements are specifically
paired with the `eventId` guard for this reason).

## 5. Unique Constraints

Where a business invariant can be expressed as a MongoDB unique index (e.g., one
`GOODS_RECEIVED` event ever creating one inward-record per `(poId, eventId)` pair), that
constraint is added as defense-in-depth alongside the `processed_events` check — not as a
replacement for it.

## 6. Metrics

- `consumer_duplicate_events_skipped_total{service, topic}`
- `consumer_events_processed_total{service, topic}`
- `consumer_processing_errors_total{service, topic}`

A sustained high duplicate-skip rate is itself a signal worth alerting on — it usually
indicates a producer or rebalance issue upstream, even though it is being handled safely.
