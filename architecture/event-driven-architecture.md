# Event-Driven Architecture

## 1. Principles

- Apache Kafka is the **only** asynchronous messaging technology in this platform.
- Kafka is **never** used as a synchronous RPC mechanism — no request/reply-over-Kafka
  patterns for user-facing latency-sensitive calls.
- Every event uses the standard envelope (below) and is versioned.
- Every consumer is idempotent (see `docs/events/idempotency-strategy.md`).
- Every publish goes through the transactional outbox (see
  `docs/events/transactional-outbox.md`) — a business transaction and its outbound event
  are committed atomically in MongoDB before an async publisher relays to Kafka.

## 2. Standard Event Envelope

```json
{
  "eventId": "UUID",
  "eventType": "INDENT_APPROVED",
  "eventVersion": 1,
  "occurredAt": "UTC timestamp (ISO-8601)",
  "producer": "indent-service",
  "correlationId": "UUID",
  "traceId": "trace-id",
  "payload": {}
}
```

Field rules:

- `eventId` — globally unique per logical event occurrence; the idempotency key for consumers.
- `eventVersion` — incremented on any breaking payload change; consumers branch on this.
- `correlationId` — stable across an entire business transaction/saga (e.g., one indent's
  full lifecycle); distinct from `traceId`, which is per-request/per-hop.
- `traceId` — OpenTelemetry trace identifier for cross-service observability.
- `payload` — event-type-specific; schemas live in `contracts/events/`.

## 3. Topic Catalogue

See `infrastructure/kafka/topics.yaml` for the authoritative, machine-readable list
(partitions, replication factor, retention). Logical topics:

```text
procurement.indent.events
procurement.po.events
procurement.inward.events
procurement.inventory.events
procurement.outward.events
procurement.return.events
procurement.asset.events
procurement.notification.events
procurement.audit.events
procurement.dashboard.events
```

Each topic has a paired retry and dead-letter topic, e.g.:

```text
procurement.indent.events
procurement.indent.events.retry
procurement.indent.events.dlt
```

## 4. Event Catalogue

| Event | Producer | Primary Consumers |
|---|---|---|
| `INDENT_SUBMITTED` | indent-service | notification-service, audit-service, dashboard-service |
| `INDENT_VERIFIED` | indent-service | notification-service, audit-service, dashboard-service |
| `INDENT_APPROVED` | indent-service | po-service, notification-service, audit-service, dashboard-service |
| `INDENT_REJECTED` | indent-service | notification-service, audit-service, dashboard-service |
| `PO_CREATED` | po-service | notification-service, audit-service, dashboard-service |
| `PO_ISSUED` | po-service | inward-service, notification-service, audit-service, dashboard-service |
| `PO_CLOSED` | po-service | audit-service, dashboard-service |
| `GOODS_RECEIVED` | inward-service | inventory-service, notification-service, audit-service, dashboard-service |
| `REJECTION_FLAGGED` | inward-service | inventory-service, notification-service, audit-service |
| `OUTWARD_ISSUED` | outward-service | inventory-service, asset-service, notification-service, audit-service, dashboard-service |
| `RETURN_RECORDED` | return-service | inventory-service, notification-service, audit-service, dashboard-service |
| `ASSET_CLOSURE_REQUESTED` | return-service | asset-service, notification-service, audit-service |
| `ASSET_CREATION_REQUESTED` | outward-service (reusable-item path) | asset-service |
| `ASSET_STATUS_CHANGED` | asset-service | notification-service, audit-service, dashboard-service |
| `LOW_STOCK_ALERT` | inventory-service | notification-service, dashboard-service |
| `SLA_BREACH` | indent-service / po-service (SLA monitors) | notification-service, dashboard-service |

Schemas for each: `contracts/events/<event-name>.schema.json`.

## 5. Partitioning Strategy

- Partition key = the primary domain entity id relevant to the event (e.g., `indentId` for
  indent events, `poId` for PO events, `itemId` for inventory events).
- This guarantees **per-entity ordering**: all events for a given indent/PO/item are
  processed in order by a single consumer within a consumer group, while still allowing
  horizontal scale across entities.
- Default: 6 partitions per topic in production (tunable per topic based on measured
  throughput), replication factor 3.

## 6. Consumer Groups

- One consumer group per **consuming service per topic** (e.g.,
  `inventory-service.procurement-inward-events`), never shared across services, so that
  each service's consumption progress is independent.
- Consumer group naming: `<service-name>.<topic-short-name>`.

## 7. Schema Evolution

- All event schemas are additive-by-default: new **optional** fields may be added without
  bumping `eventVersion`.
- Removing a field, changing a field's type, or changing required-ness is a **breaking**
  change — requires an `eventVersion` bump, and the producer must (for a documented
  transition window) publish both versions or ensure all consumers are upgraded first
  (consumer-driven contract testing gates this in CI — see `contracts/schemas/`).
- Schema Registry (Confluent-compatible, deployed alongside Kafka) enforces
  `BACKWARD`-compatible evolution for Avro/JSON Schema-registered topics.

## 8. Ordering, Offsets, Retention, Replay

- **Ordering:** guaranteed per-partition only; partition key selection (§5) is what
  delivers per-entity ordering.
- **Offset management:** consumers commit offsets only after successful processing
  (at-least-once delivery) — combined with idempotent processing this yields effectively-once
  semantics at the application level.
- **Retention:** primary topics retain 7 days (configurable per topic in
  `infrastructure/kafka/topics.yaml`); DLT topics retain 30 days to allow investigation and
  replay.
- **Replay strategy:** operators replay from a DLT (or from an earlier offset on the
  primary topic) using a documented, audited replay job — never by blindly resetting a
  production consumer group's offset without a runbook. See
  `docs/runbooks/kafka-dlt-replay.md`.

## 9. Poison Message Handling

```text
Producer → Topic → Consumer → processing failure
                                     │
                                     ▼
                             Retry topic (bounded retries, backoff)
                                     │
                                retries exhausted
                                     │
                                     ▼
                             Dead Letter Topic (+ alert)
```

A message that fails deserialization or repeatedly fails business processing is moved to
its topic's `.dlt` topic with the original payload plus failure metadata
(`failureReason`, `failureCount`, `firstFailedAt`), and triggers a Prometheus alert on DLT
volume (see `infrastructure/monitoring/`).
