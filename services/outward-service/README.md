# Outward Service

This version adds synchronous Master Data validation to the outward request workflow.

## Validation performed

Before a request is persisted:

- Every requested item must exist in Master Data.
- Every requested item must be `ACTIVE`.
- Requested quantity must not exceed the current `availableStock` returned by Master Data.
- The supplied `departmentName` must match an `ACTIVE` Master Data department.
- An optional `projectId` must identify an `ACTIVE` Master Data project.
- Duplicate item IDs in one request are rejected.
- Asset tracking is derived from Master Data; the client cannot declare it.

The service communicates with Master Data through its documented REST APIs and never reads the Master Data MongoDB directly.

## Downstream configuration

```text
MASTER_DATA_BASE_URL=http://master-data-service:8082
MASTER_DATA_CONNECT_TIMEOUT_MS=2000
MASTER_DATA_READ_TIMEOUT_MS=3000
```

The authenticated bearer token, correlation ID and W3C `traceparent` are propagated to the synchronous Master Data request.

## Error codes

Examples include:

- `OUTWARD_ITEM_NOT_FOUND`
- `OUTWARD_ITEM_INACTIVE`
- `OUTWARD_INSUFFICIENT_STOCK`
- `OUTWARD_DEPARTMENT_NOT_FOUND`
- `OUTWARD_PROJECT_NOT_FOUND`
- `OUTWARD_PROJECT_INACTIVE`
- `MASTER_DATA_SERVICE_UNAVAILABLE`

A Master Data availability failure returns HTTP 503. A business validation failure returns HTTP 400.

## Boundary

This service does not modify inventory, create an issue transaction, publish `OUTWARD_ISSUED`, or create assets in this phase.

## Verification

From the repository root:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:test
```

Then:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:build
```


## Approval workflow

The service now supports the authenticated approval lifecycle:

- `POST /api/v1/outward/{outwardNumber}/submit` — requester submits a `DRAFT` request.
- `POST /api/v1/outward/{outwardNumber}/approve` — configured approval role changes `PENDING_APPROVAL` to `APPROVED`.
- `POST /api/v1/outward/{outwardNumber}/reject` — configured approval role changes `PENDING_APPROVAL` to `REJECTED`.

Approval roles are configured with `OUTWARD_APPROVAL_ROLES` and default to `TL,MD`. JWT roles are read from the `roles` claim and mapped to Spring `ROLE_*` authorities.

The service never modifies inventory stock during approval. Inventory mutation and the `OUTWARD_ISSUED` event remain later workflow responsibilities.


## Issue transaction

The service now supports issuing an approved outward request:

- `POST /api/v1/outward/{outwardNumber}/issue` — creates the issue transaction and changes the request from `APPROVED` to `ISSUED`.
- The issuer is taken from the authenticated JWT identity.
- The issue transaction captures the approved request snapshot, recipient, approver, issuer, items, quantities, project, department and purpose.
- A unique `outwardNumber` prevents more than one issue transaction for the same outward request.
- The operation uses MongoDB transaction management so creation of the issue transaction and the request state transition are committed together when MongoDB transaction support is available (for example MongoDB Atlas replica sets).
- This phase does not modify inventory MongoDB and does not publish `OUTWARD_ISSUED`; those are later integration responsibilities.

Issue errors include `OUTWARD_NOT_APPROVED`, `OUTWARD_ALREADY_ISSUED`, `OUTWARD_CONCURRENT_MODIFICATION`, and `OUTWARD_NUMBER_REQUIRED`.

## Phase 08 — Idempotency & Optimistic Concurrency

The issue endpoint accepts an optional `Idempotency-Key` header. When supplied, the key is persisted on the issue transaction with a sparse unique MongoDB index. Retrying the same key for the same outward request returns the original issue transaction; reusing the key for another outward request is rejected. When omitted, the issue is processed normally without idempotency-key replay protection. The outward request continues to use optimistic locking through `@Version`, and concurrent modifications are returned as `OUTWARD_CONCURRENT_MODIFICATION`.

Example:
```http
POST /api/v1/outward/OUT%2F26-27%2F001/issue
Authorization: Bearer <jwt>
Idempotency-Key: issue-OUT-001-20260929
```

Phase 08 still does not publish `OUTWARD_ISSUED` or directly update inventory. Those responsibilities remain in later phases.

## Phase 09 — Transactional Outbox

The issue workflow now records the integration event in an `outbox_events` collection within the same MongoDB transaction as the issue transaction and `APPROVED -> ISSUED` state transition. The event is stored as `PENDING` and contains a versioned `OUTWARD_ISSUED` JSON payload with the outward number, issuer, approver, recipient, department, project, purpose, asset-tracked flag and issued items.

This phase deliberately does not publish to Kafka or start an outbox polling publisher. Phase 10 will consume pending outbox records and publish the `OUTWARD_ISSUED` Kafka event. If the outbox write fails, the enclosing transaction fails rather than leaving an issued business record without its durable event record.

## Phase 10 — OUTWARD_ISSUED Kafka Event

Phase 10 adds the outbox publisher that delivers persisted `OUTWARD_ISSUED` events to Kafka. The message key is the exact encoded `outwardNumber`, and the message value is the versioned JSON payload stored by the transactional outbox. The outbox is marked `PUBLISHED` only after Kafka acknowledgement. Failed delivery remains retryable until the configured maximum attempts.

Default topic: `outward.events`. Configure with `OUTWARD_ISSUED_TOPIC`.


## Phase 11 — Audit Integration

Outward state-changing actions emit immutable `AUDIT_EVENT` records through the existing transactional outbox.
The audit payload carries event identity, entity, action, actor, role, timestamp, correlation/trace ID, source service, before/after state and rejection reason where applicable.
Audit events are published to the configurable `audit.events` Kafka topic. No audit update/delete API is exposed by outward-service.

Configuration: `AUDIT_EVENTS_TOPIC` (default `audit.events`).


## Phase 12 — Notification Hooks

The outward service emits notification-triggering business events for approval and rejection through the transactional outbox. The separate `notification-service` owns notification persistence and delivery. Outward-service does not access `notificationdb` or implement notification delivery.

Events:
- `OUTWARD_APPROVED`
- `OUTWARD_REJECTED`

Topic: `${NOTIFICATION_EVENTS_TOPIC:notification.events}`


## Phase 13 — Observability

The outward service now exposes production observability through Spring Boot Actuator, Micrometer, Prometheus, Micrometer tracing and OpenTelemetry.

### Metrics and observations

Business operations are instrumented with Micrometer observations for:

- outward request creation
- outward submission
- approval
- rejection
- issue transaction
- Master Data validation and downstream lookups
- transactional outbox publishing batches

Prometheus metrics are available at `/actuator/prometheus`; the generic metrics endpoint is `/actuator/metrics`.

### Tracing

- W3C trace propagation is enabled.
- REST calls to Master Data continue to propagate `traceparent` and `X-Correlation-Id`.
- Kafka template observation is enabled so producer spans can participate in distributed tracing.
- OTLP export is configurable with `OTEL_EXPORTER_OTLP_ENDPOINT`.

### Correlation and logs

Every HTTP request gets an `X-Correlation-Id` when one is not supplied. The value is returned in the response and stored in MDC. Console logs include `traceId`, `spanId`, and `correlationId`.

Sensitive authentication material such as bearer tokens is not logged.

### Health and readiness

Actuator health probes are enabled, including liveness/readiness endpoints under `/actuator/health/**`.

### Verification

From the repository root:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:test
```

Then:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:build
```
