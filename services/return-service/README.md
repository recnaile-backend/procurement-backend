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

## Upcoming Phases

- Phase 3 — Return State Machine — Completed
- Phase 4 — Create Return Request — Completed
- Phase 5 — Issued-Quantity / Asset Validation
- Phase 6 — Return Verification Workflow
- Phase 7 — Good / Damaged Return Processing
- Phase 8 — Kafka Events & Event Contracts
- Phase 9 — Transactional Outbox & Event Reliability
- Phase 10 — Inventory & Asset Integration
- Phase 11 — Security, RBAC & Idempotency
- Phase 12 — Audit & Notification Integration
- Phase 13 — Observability & Operational Readiness
- Phase 14 — Testing, OpenAPI & API Hardening
- Phase 15 — Docker/Kubernetes & Final Integration
