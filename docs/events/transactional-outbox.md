# Transactional Outbox Pattern

## 1. Problem

A naive implementation commits a MongoDB business-write and then separately calls the
Kafka producer. If the process crashes (or Kafka is briefly unreachable) between those two
steps, the business state changes but the corresponding domain event is **never
published** — every downstream consumer (inventory, notification, audit, dashboard) silently
drifts out of sync with reality. This platform never allows that failure mode.

## 2. Design

```text
Business Transaction
       │
       ▼
MongoDB (single multi-document ACID transaction using a replica-set-backed session)
 ┌─────────────────────────────┐
 │ Business Record (e.g. Indent)│
 │ Outbox Event (same txn)      │
 └─────────────────────────────┘
       │
       ▼
Outbox Publisher (separate process/thread, polling or change-stream-driven)
       │
       ▼
Kafka
       │
       ▼
Consumer
```

The business write and the outbox event write happen in the **same MongoDB transaction**
— they succeed or fail together. Kafka publication is a separate, retryable step performed
by an Outbox Publisher that reads unpublished outbox rows.

## 3. Outbox Collection Shape

Each service's own database (never a shared collection) holds an `outbox_events`
collection:

```json
{
  "_id": "ObjectId",
  "eventId": "UUID",
  "eventType": "INDENT_APPROVED",
  "eventVersion": 1,
  "aggregateId": "indent-id",
  "topic": "procurement.indent.events",
  "payload": { },
  "status": "PENDING | PUBLISHED | FAILED",
  "createdAt": "timestamp",
  "publishedAt": "timestamp | null",
  "attempts": 0,
  "lastError": "string | null"
}
```

## 4. Publisher Mechanics

Two supported implementation strategies (service teams choose one when implementing a
given domain service; documented here so the choice is consistent within a service):

1. **Polling publisher** — a scheduled task queries `status = PENDING` (indexed), publishes
   to Kafka, and on broker ack updates `status = PUBLISHED`. Simple, low-dependency, small
   polling latency (sub-second interval is typical).
2. **Change-stream publisher** — a MongoDB Change Stream on `outbox_events` triggers
   near-real-time publication. Lower latency, slightly higher operational complexity.

Either way, publishing is **at-least-once**: a crash between "Kafka ack received" and
"status updated to PUBLISHED" results in a duplicate publish on restart — which is exactly
why every consumer must be idempotent (see `docs/events/idempotency-strategy.md`).

## 5. Publishing Guarantees

- **Never lost:** the event exists durably in MongoDB before any Kafka interaction is
  attempted.
- **At-least-once delivered:** retried on any publish failure, using the same `eventId` so
  downstream idempotency handles duplicates safely.
- **Ordered per aggregate:** the publisher processes a given `aggregateId`'s pending events
  in creation order and uses `aggregateId` as the Kafka partition key.

## 6. Retry & Failure

- Transient publish failures increment `attempts` and retry with backoff.
- After a configured max-attempt threshold, `status` is set to `FAILED` and an alert fires
  (Prometheus metric `outbox_publish_failures_total`) for operator investigation —
  the event is never silently dropped.

## 7. Duplicate Handling

Downstream idempotency (keyed on `eventId`) is the system of record for "already
processed" — the outbox pattern does not attempt to guarantee exactly-once delivery itself,
only durable at-least-once delivery paired with idempotent consumption.

## 8. Cleanup & Retention

- `PUBLISHED` rows are retained for a bounded window (default 7 days) for
  audit/troubleshooting, then purged by a scheduled housekeeping job.
- `FAILED` rows are retained until an operator resolves and manually requeues or archives
  them (never auto-purged).

## 9. Monitoring

- `outbox_pending_count` (gauge, per service) — alerts if pending backlog grows
  unexpectedly, indicating a stuck publisher.
- `outbox_publish_latency_seconds` (histogram) — time from `createdAt` to `publishedAt`.
- `outbox_publish_failures_total` (counter).
