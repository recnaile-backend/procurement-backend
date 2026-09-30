# Return Service

RECNAILE Procurement Platform Return Service.

## Service Overview

- Service name: `return-service`
- Port: `8088`
- Gateway route: `/api/v1/returns/**`
- Database: MongoDB Atlas, database `return`
- Collection introduced in Phase 2: `return_requests`
- Messaging: Kafka
- Security foundation: Spring Security + OAuth2 Resource Server dependencies
- Observability foundation: Actuator, Prometheus metrics and OpenTelemetry/Micrometer tracing

## Architecture

The service follows the platform standard:

```text
Controller → Service/Application → Repository → MongoDB
                         ↓
                  Kafka / Outbox
```

Controllers remain transport/validation focused. Business state transitions belong in the application/service layer, repositories own persistence, and cross-service databases are never queried directly.

## Phase Status

### Phase 1 — Foundation & Service Setup

Completed.

- Spring Boot service foundation
- Java 21 baseline
- Gradle service configuration
- MongoDB configuration
- Kafka configuration
- Security dependencies
- Actuator health/metrics
- Prometheus metrics
- OpenTelemetry/Micrometer tracing foundation
- Environment-based configuration
- Dockerfile
- Context startup test
- Generic CRUD scaffold removed

### Phase 2 — Return Domain Model & Enums

Completed.

Implemented the explicit Return aggregate model and persisted lifecycle/condition enums required by the documented workflow.

#### Return aggregate

`com.recnaile.returns.model.entity.ReturnRequest`

Fields:

- `returnId` — generated Return identifier and MongoDB document ID
- `outwardId` — originating outward issue
- `outwardLineId` — specific issued item row within the outward request
- `itemId` — product/SKU identifier
- `quantity` — returned quantity
- `assetId` — optional reusable-asset reference
- `returnedBy` — authenticated return initiator reference
- `condition` — return condition
- `remarks` — optional return remarks
- `status` — persisted lifecycle state
- `verifiedBy` — verification actor, populated during verification
- `verifiedAt` — verification timestamp
- `createdAt` — creation timestamp
- `updatedAt` — last-update timestamp
- `version` — optimistic-concurrency version

The model uses explicit fields and does not use a generic payload map.

#### Return condition

`ReturnCondition`:

- `GOOD`
- `DAMAGED`

These values follow the documented rule that a good return is restored to available stock and a damaged return is placed into defect stock.

#### Return status

`ReturnStatus`:

- `PENDING_VERIFICATION`
- `VERIFIED`

The status enum defines the persisted lifecycle states. State-transition enforcement is intentionally implemented in Phase 3.

#### MongoDB indexes

Phase 2 defines indexes for the primary Return query/reference patterns:

- outward + outward line
- status + creation time
- outward ID
- outward line ID
- item ID
- asset ID
- returned-by
- creation time

### Phase 3 — Return State Machine

Completed.

The lifecycle state machine is implemented in `com.recnaile.returns.service.ReturnStateMachine`. The documented persisted transition is:

```text
PENDING_VERIFICATION → VERIFIED
```

Rules enforced by the state machine:

- A return can be verified only while it is `PENDING_VERIFICATION`.
- A verified return cannot be verified again.
- A verified return cannot move back to `PENDING_VERIFICATION`.
- Null aggregates and null target states are rejected.
- Invalid transitions raise `InvalidReturnStateTransitionException`.
- The transition updates `updatedAt`.
- Good/damaged stock processing and downstream event publication are intentionally deferred to their later phases.

The state machine is kept in the application/service layer; the entity does not contain lifecycle decision logic.

## Business Rules Covered by the Model

The documented Return workflow requires a Return to contain the item and quantity, identify who returned it, capture condition and remarks, and link the return to the issued item/asset where applicable. Verification actor and timestamp are retained on the aggregate.

The model does not perform verification, stock movement, event publication, authorization, or state-transition decisions. Those responsibilities belong to later application/integration phases.

## Current API Status

Business REST endpoints are not implemented yet. They are introduced in the appropriate workflow/API phases.

## Current Event Status

Return Kafka events are not implemented yet. They are introduced after the domain/state workflow is established.

## Configuration

Production/deployment configuration must provide platform-managed values such as:

- `MONGODB_BASE_URI`
- `MONGODB_DATABASE`
- `KAFKA_BOOTSTRAP_SERVERS`
- `SERVICE_PORT`
- `TRACING_SAMPLE_RATE`
- `OTEL_EXPORTER_OTLP_ENDPOINT`

No credentials are stored in source control.

## Verification

From the repository root:

```bash
./gradlew :services:return-service:clean :services:return-service:test
./gradlew :services:return-service:build
```

For the local container build:

```bash
docker compose up -d --build return-service
```

### Phase 4 — Create Return Request

Completed.

Implemented the first business REST workflow for creating a Return request.

#### API

`POST /api/v1/returns` accepts an explicit `CreateReturnRequest` payload containing:

- `outwardId` — originating outward issue
- `outwardLineId` — specific issued item row
- `itemId` — product/SKU identifier
- `quantity` — positive returned quantity
- `assetId` — optional reusable-asset reference
- `returnedBy` — return initiator reference
- `condition` — `GOOD` or `DAMAGED`
- `remarks` — optional remarks, maximum 2000 characters

A successful request returns HTTP `201 Created` using the platform response envelope:

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

New returns are persisted with:

- a generated Return ID
- `PENDING_VERIFICATION` status
- creation and update timestamps
- the submitted item, quantity, outward linkage, actor, condition and remarks

#### Application and persistence layers

- `ReturnRequestController` handles HTTP transport and validation only.
- `ReturnRequestService` creates the Return aggregate and owns creation business behavior.
- `ReturnRequestRepository` provides MongoDB persistence through Spring Data.
- Request fields are explicit; no generic payload map is used.
- Input strings are trimmed and blank optional values are normalized to `null`.

Issued-quantity/asset existence validation, authentication/RBAC enforcement, idempotency, verification, stock movement, events and outbox reliability remain in their later roadmap phases.

#### Error handling

Validation failures use the stable error envelope with `VALIDATION_ERROR`. Invalid creation arguments use `INVALID_RETURN_REQUEST`. Trace/correlation-aware error enrichment remains part of later operational/API hardening work.

#### Tests

Phase 4 adds tests for:

- successful Return creation and persistence mapping
- generated Return ID and initial `PENDING_VERIFICATION` state
- timestamp handling
- optional asset/remarks normalization
- successful `201 Created` REST response
- request validation failure response

### Phase 5 — Issued-Quantity / Asset Validation

Completed.

Return creation now validates the requested return against the originating issued outward line before persistence. The Return Service uses a synchronous integration boundary to the Outward Service and does not query another service database directly.

#### Validation flow

```text
Create Return Request
        ↓
IssuedReturnValidationService
        ↓
Outward Service validation client
        ↓
Validate outward + line + item + quantity + asset
        ↓
Persist Return Request
```

The validation checks:

- The supplied `outwardId` matches the issued outward record.
- The supplied `outwardLineId` matches the specific issued line.
- The supplied `itemId` matches the item on the issued line.
- The requested return quantity is greater than zero and does not exceed the remaining issued quantity.
- Remaining quantity is calculated as issued quantity minus already returned quantity.
- When the issued line has an `assetId`, the return must provide the same asset ID.
- A return cannot introduce an unrelated asset when the issued line is not asset-linked.

#### Integration boundary

`IssuedReturnValidationClient` is the application integration boundary. `OutwardServiceRestClient` is the HTTP implementation. The Outward Service base URL is configured with:

```text
OUTWARD_SERVICE_URL
```

Default container value:

```text
http://outward-service:8087
```

The client uses the return-validation resource:

```text
GET /api/v1/outwards/{outwardId}/lines/{outwardLineId}/return-validation
```

The response supplies the issued outward/line identity, item ID, issued quantity, already-returned quantity and optional asset ID. Cross-service database access is not used.

#### Persistence behavior

Validation occurs before `ReturnRequestRepository.save(...)`. A failed validation therefore does not create a Return document. Existing Phase 4 creation behavior remains unchanged after validation succeeds.

#### Tests

Phase 5 adds coverage for:

- valid return quantity within remaining issued quantity
- quantity exceeding remaining issued quantity
- no remaining quantity
- item mismatch
- asset mismatch
- matching asset
- service creation continuing to persist only after successful validation

Authentication/RBAC, idempotency, verification, stock movement, Kafka events and transactional outbox reliability remain in their later roadmap phases.


### Phase 6 — Return Verification Workflow

Completed.

Implemented the documented verification workflow for a submitted Return request.

#### Verification API

`POST /api/v1/returns/{returnId}/verify`

Request body:

```json
{
  "verifiedBy": "procurement-user-1"
}
```

A successful verification returns HTTP `200 OK` using the standard response envelope and updates the Return aggregate to `VERIFIED`.

#### Verification behavior

- Loads the Return request from the Return Service's own MongoDB repository.
- Rejects a blank Return ID.
- Rejects a missing Return request with HTTP `404` and error code `RETURN_NOT_FOUND`.
- Rejects a blank verification actor.
- Uses the Phase 3 state machine, so only `PENDING_VERIFICATION → VERIFIED` is allowed.
- Rejects duplicate verification of an already verified return.
- Records `verifiedBy` and `verifiedAt`.
- Updates `updatedAt`.
- Persists the verified aggregate only after the state transition succeeds.

#### Separation of responsibilities

- `ReturnRequestController` handles HTTP transport and request validation.
- `ReturnVerificationService` owns the verification use case.
- `ReturnStateMachine` remains the authority for lifecycle transition validity.
- `ReturnRequestRepository` owns MongoDB persistence.
- Authentication/RBAC enforcement for the verifier is intentionally deferred to Phase 11, where JWT validation and endpoint/entity authorization are implemented together.
- Good/damaged inventory processing and reusable-asset updates are intentionally deferred to Phase 7 and Phase 10.
- Kafka events and transactional outbox reliability are intentionally deferred to Phases 8 and 9.

#### Tests

Phase 6 adds coverage for:

- successful pending-to-verified transition
- verification actor and timestamp persistence
- unknown Return ID handling
- duplicate verification rejection
- blank verifier rejection
- successful verification REST response

No stock movement, asset lifecycle mutation, Kafka publication, notification or audit integration is performed by Phase 6.

## Upcoming Phases

- Phase 3 — Return State Machine — Completed
- Phase 4 — Create Return Request — Completed
- Phase 5 — Issued-Quantity / Asset Validation — Completed
- Phase 6 — Return Verification Workflow — Completed
- Phase 7 — Good / Damaged Return Processing
- Phase 8 — Kafka Events & Event Contracts
- Phase 9 — Transactional Outbox & Event Reliability
- Phase 10 — Inventory & Asset Integration
- Phase 11 — Security, RBAC & Idempotency
- Phase 12 — Audit & Notification Integration
- Phase 13 — Observability & Operational Readiness
- Phase 14 — Testing, OpenAPI & API Hardening
- Phase 15 — Docker/Kubernetes & Final Integration

### Phase 7 — Good / Damaged Return Processing

Completed.

Implemented the Return Service's condition-based processing state transition after successful verification.

#### Processing API

`POST /api/v1/returns/{returnId}/process`

The endpoint does not accept a client-supplied condition. It uses the condition already recorded on the Return aggregate, preventing a caller from changing the business outcome during processing.

#### Processing rules

- Only a `VERIFIED` return can be processed.
- `GOOD` condition transitions to `ACCEPTED_GOOD`.
- `DAMAGED` condition transitions to `ACCEPTED_DAMAGED`.
- A pending return cannot bypass verification.
- An already accepted return cannot be processed again.
- Unknown Return IDs return `RETURN_NOT_FOUND`.
- The Return aggregate's `updatedAt` is updated using the service clock.
- The processed aggregate is persisted only after a valid state transition.

The documented business outcome is preserved: good returns are intended to restore available stock, while damaged returns are intended to increase defect stock. Actual Inventory Service stock mutation is deliberately deferred to Phase 10, and Kafka event publication is deferred to Phase 8/9. This phase therefore establishes the authoritative Return Service state without directly changing Inventory or Asset data.

#### State machine additions

The persisted lifecycle now supports:

```text
PENDING_VERIFICATION → VERIFIED → ACCEPTED_GOOD
                                      └→ ACCEPTED_DAMAGED
```

No reverse transitions or duplicate processing transitions are allowed.

#### Tests

Phase 7 adds coverage for:

- verified GOOD return → `ACCEPTED_GOOD`
- verified DAMAGED return → `ACCEPTED_DAMAGED`
- processing before verification rejection
- duplicate processing rejection
- unknown Return ID handling
- processing timestamp update
- successful processing REST response

Inventory stock changes, reusable asset lifecycle changes, Kafka events, transactional outbox, RBAC and idempotency remain in their designated later phases.

## Phase 8 — Kafka Events & Event Contracts

Phase 8 adds the Return Service business-event boundary. After a return is successfully persisted in its final accepted state, the service publishes a `RETURN_RECORDED` Kafka event.

### Event payload

The business payload contains the documented return data:
- `returnId`
- `itemId`
- `qty`
- `condition`

The event also carries `eventId`, `eventType`, and `occurredAt` metadata for traceability.

### Topic

The topic is configurable through `app.kafka.topics.return-recorded` and defaults to `return.recorded`.

### Reliability boundary

Phase 8 establishes Kafka publishing and the event contract. Transactional outbox and durable publish/retry semantics are intentionally deferred to Phase 9. The event is published only after the MongoDB save returns successfully.

### Architecture rule

Return Service does not directly update Inventory or Asset databases. Downstream services consume the return event and apply their own state changes.

### Phase 9 — Transactional Outbox & Event Reliability

Completed.

Phase 9 replaces the direct Kafka publication from the Return processing transaction with a durable transactional outbox. The accepted Return state and its `RETURN_RECORDED` event record are written to MongoDB in the same transaction.

#### Transactional processing flow

```text
Verified Return
      ↓
ReturnProcessingService
      ↓
MongoDB transaction
   ┌──┴──────────────────────┐
   │                         │
Save ReturnRequest     Save ReturnEventOutbox
   │                         │
   └──────────────┬──────────┘
                  ↓
             COMMIT
                  ↓
        Outbox Dispatcher
                  ↓
              Kafka
```

#### Outbox document

`return_event_outbox` stores the durable event record with:

- `eventId` — stable event identifier used across retries
- `aggregateId` — Return ID
- `eventType` — `RETURN_RECORDED`
- `topic` — configured Kafka topic
- `returnId`
- `itemId`
- `qty`
- `condition`
- `occurredAt`
- `status` — `PENDING` or `SENT`
- `attempts`
- `createdAt`
- `publishedAt`
- `lastError`
- optimistic-concurrency `version`

A unique index on `eventId` prevents duplicate outbox records for the same event identifier.

#### Dispatcher behavior

`ReturnEventOutboxDispatcher` periodically reads pending records and publishes them to Kafka. A record is marked `SENT` only after the Kafka send future completes successfully. If publication fails, the record remains `PENDING`, the attempt counter is incremented and the error is retained for the next dispatch cycle.

The dispatcher uses the same stable `eventId` when reconstructing `RETURN_RECORDED`, so a retry does not generate a new event identity. Downstream consumers should therefore use event IDs for idempotent processing according to the platform event/idempotency standards.

#### MongoDB transaction configuration

`MongoTransactionManager` is registered as the service transaction manager and `ReturnProcessingService.process(...)` is annotated with `@Transactional`. The Return document and outbox document are therefore committed together when the deployment's MongoDB topology supports transactions (such as MongoDB Atlas or a configured replica set).

#### Kafka boundary

The processing service no longer calls Kafka directly. Kafka publication is performed only by the outbox dispatcher after the MongoDB transaction has committed. This prevents a successful Return transaction from losing its event merely because Kafka was temporarily unavailable.

#### Tests

Phase 9 adds coverage for:

- Return processing enqueues an outbox record instead of directly publishing Kafka.
- Outbox records start in `PENDING` state.
- Stable event IDs are retained in the outbox payload.
- Successful Kafka publication marks an outbox record `SENT`.
- Failed Kafka publication leaves the record `PENDING` and records an attempt/error.
- Existing Return state, verification, validation and Kafka contract tests remain part of the suite.

## Updated Upcoming Phases

- Phase 11 — Security, RBAC & Idempotency
- Phase 12 — Audit & Notification Integration
- Phase 13 — Observability & Operational Readiness
- Phase 14 — Testing, OpenAPI & API Hardening
- Phase 15 — Docker/Kubernetes & Final Integration

## Phase 10 — Inventory Integration Contract

Completed for the Inventory side of the Return workflow. The Return Service remains the authoritative source of the accepted return outcome and propagates it through the existing `RETURN_RECORDED` transactional-outbox event.

### Inventory behavior represented by the event

- `GOOD` return → Inventory increases **available stock** by the returned quantity.
- `DAMAGED` return → Inventory increases **defect stock** by the returned quantity.
- `returnId` provides the transaction identity for downstream idempotency.
- `itemId` identifies the stock item.
- `qty` is the server-calculated returned quantity.
- `condition` determines the downstream stock bucket.

### Integration boundary

Return Service does **not** directly query or modify Inventory MongoDB. It does not make an assumed synchronous stock-adjustment call. The existing `RETURN_RECORDED` event is the asynchronous integration boundary, and the Phase 9 transactional outbox guarantees that the event is durably queued with the accepted Return state.

This keeps the database-per-service rule intact and allows the already-completed Inventory Service to consume the event using its own transaction and idempotency rules.

### Asset-service dependency

The Asset Service has not yet been implemented in the project. Therefore Phase 10 intentionally does **not** add an Asset REST client, Kafka consumer, asset database dependency, or guessed Asset API. Reusable-asset lifecycle integration will be completed after Asset Service is available.

### Tests

Phase 10 adds Inventory integration contract tests verifying that `RETURN_RECORDED` carries the exact item, quantity, condition, return identity and occurrence timestamp required by Inventory.


## Phase 11 — Security, RBAC & Idempotency

Phase 11 adds authenticated JWT resource-server security, endpoint-level role authorization and retry-safe idempotency for all state-changing Return POST operations.

### Security

- JWT Resource Server validation is enabled with a configurable JWK Set URI.
- Roles are read from the JWT `roles` claim (or a single `role` claim) and normalized to `ROLE_*`.
- Return creation is available to `EMPLOYEE`, `PROCUREMENT`, `TL` and `MD`.
- Verification is restricted to `PROCUREMENT`, `TL` and `MD`.
- Final good/damaged processing is restricted to `PROCUREMENT`, `TL` and `MD`.
- Unauthenticated requests return `401 UNAUTHENTICATED`.
- Authenticated users without the required role return `403 FORBIDDEN`.
- The authenticated JWT principal is the authoritative actor; the `returnedBy` / `verifiedBy` payload values must match it rather than overriding it.
- No raw identity headers are trusted.

### Idempotency

State-changing POST endpoints require `Idempotency-Key`. Keys are persisted in `return_idempotency_records` with the operation, authenticated actor, request fingerprint and resulting Return ID. Repeating the same key with the same actor and request returns the original Return instead of applying the business transition again. Reusing a key with different operation, actor or request data returns `409 IDEMPOTENCY_CONFLICT`.

The implementation keeps idempotency state in Return Service's own MongoDB and does not query another service's database.


### Phase 11 — Security, RBAC & Optional Idempotency

`Idempotency-Key` is optional for return create, verification, and processing endpoints. When supplied, the key is validated and replay/conflict protection is applied. When omitted, the business operation proceeds without an idempotency record. Authenticated JWT identity remains authoritative for actor fields.
