# Purchase Order Service

Authoritative owner of purchase orders in the RECNAILE Procurement Platform.

- Port: `8084`
- MongoDB database: `purchase_order`
- Gateway prefix: `/api/v1/purchase-orders/**`
- Java: 21
- Spring Boot: repository-managed Spring Boot 3.x

## Phase 1 — Foundation

Phase 1 establishes the service runtime, MongoDB/Kafka configuration, Actuator,
correlation ID propagation, standardized API envelope/error model, graceful shutdown,
and the package boundaries required for later PO domain implementation.

No generic CRUD endpoint is retained. Business APIs are introduced only with their
corresponding PO domain model and documented contract in later phases.

## Phase 2 — PO Domain Model

The service now contains the explicit Purchase Order aggregate and embedded line model,
with lifecycle status, source-indent references, vendor/project/department references,
server-calculated monetary fields, delivery/invoice metadata, audit timestamps and
optimistic-concurrency versioning.

MongoDB indexes are defined for source indent lookup, vendor/status, department/status,
project/status, and status/creation-time queries. Business calculations and lifecycle
transition rules remain outside the persistence model and will be implemented in later
phases.


## Required environment

- `MONGODB_BASE_URI`
- `MONGODB_DATABASE` (default: `purchase_order`)
- `KAFKA_BOOTSTRAP_SERVERS` (default: `kafka:29092`)
- `PURCHASE_ORDER_SERVICE_PORT` (default: `8084`)
- `OTEL_EXPORTER_OTLP_ENDPOINT` (default: `http://otel-collector:4317`)
- `TRACING_SAMPLE_RATE` (default: `0.1`)
- `BUSINESS_TIMEZONE` (default: `Asia/Kolkata`)

Do not commit credentials or secrets.


## Phase 5 — PO Number Generation

PO IDs are generated server-side in the format `PO%2F26-27%2F001`, `PO%2F26-27%2F002`, `PO%2F26-27%2F003`, ... . MongoDB atomically increments one sequence document. Clients cannot supply or override the PO ID.

## Phase 7 REST APIs

Base path: `/api/v1/purchase-orders`

- `POST /from-approved-indent` — create a PO from a validated READY_FOR_PO indent snapshot.
- `GET /{poId}` — get a PO by ID.
- `GET /` — list POs with optional `status`, `vendorName`, `projectId`, and `departmentName` filters.
- `POST /{poId}/issue` — transition DRAFT -> ISSUED.
- `POST /{poId}/cancel` — transition to CANCELLED where allowed.
- `POST /{poId}/complete` — transition to COMPLETED where allowed.

Authentication/RBAC is intentionally handled in Phase 8; this phase does not introduce raw identity headers.

## Phase 8 — Security & RBAC

The service is a stateless OAuth2 Resource Server using the platform JWT signing configuration. JWT roles are
read from the `roles` claim and normalized to Spring `ROLE_*` authorities.
The documented roles are Employee, Procurement, TL and MD.

Purchase-order access follows the documented module responsibilities:
- Employee: no PO API access.
- Procurement: create, view, issue/send, cancel and complete POs.
- TL: view POs.
- MD: full PO access.

Configure:
- `JWT_SECRET_KEY` — shared signing secret used by the current Auth Service and PO Service deployment.
- `APP_SECURITY_JWT_ISSUER_URI` — optional issuer validation value.

Only Actuator health/info are public. All PO APIs require a valid Bearer token.
Raw identity headers are not trusted; the authenticated JWT subject is used as the actor ID.
Entity-level authorization and retry/idempotency controls are strengthened in later phases.


## Phase 9 — Idempotency & Optimistic Concurrency

Implemented:
- `Idempotency-Key` is optional on retry-safe PO POST commands.
- MongoDB-backed idempotency records are scoped by authenticated actor + key.
- Request fingerprints use SHA-256 and include operation/resource/request data.
- Reusing a key with a different request returns `409 IDEMPOTENCY_KEY_REUSED`.
- Concurrent reuse while the original request is running returns `409 IDEMPOTENCY_REQUEST_IN_PROGRESS`.
- Successful retries replay the previously created/updated PO resource.
- `PurchaseOrder.version` uses Spring Data MongoDB `@Version`.
- Stale lifecycle writes are mapped to `409 PO_CONCURRENT_MODIFICATION`.
- `sourceIndentIds` has a unique MongoDB multikey index to prevent duplicate PO conversion of the same indent.
- Controller contains transport concerns only; lifecycle persistence now belongs to `PurchaseOrderLifecycleService`.
- All service classes remain under the single top-level `com.recnaile.purchaseorder.service` package.
- Roles remain `Employee`, `Procurement`, `TL`, and `MD`; no Admin role is introduced.

### Optional headers
Command POSTs may include:
`Idempotency-Key: <unique-client-key>`

### Mongo index note
`uk_source_indent` is a unique multikey index. If an older non-unique `idx_source_indent` index already exists in an existing database, drop that obsolete index before enabling automatic index creation for the new unique index.

## Phase 10 — Kafka Events

The Purchase Order Service defines the documented purchase-order domain events:

- `PO_CREATED` after a Purchase Order is successfully persisted.
- `PO_ISSUED` after a Purchase Order transitions to `ISSUED` and is persisted.
- 
Event envelope fields follow the platform event guidance: `eventId`, `eventType`, `eventVersion`,
`occurredAt`, `producerService`, `aggregateType`, `aggregateId`, `correlationId`, `causationId`,
`actorId`, `actorRole`, `schemaVersion`, and a typed Purchase Order payload.

Default Kafka topic:

`procurement.purchase-order.events`

Override with:

`PURCHASE_ORDER_EVENTS_TOPIC`

Producer reliability settings use `acks=all`, Kafka idempotence, compression, retries and a bounded
delivery timeout.


## Phase 11 — Transactional Outbox

Kafka publication now uses a MongoDB-backed transactional outbox.

- Purchase Order state changes and their corresponding outbox records are persisted in the same MongoDB transaction.
- `PO_CREATED` and `PO_ISSUED` are stored as `PENDING` outbox events before Kafka publication.
- A scheduled outbox publisher sends pending events to `procurement.purchase-order.events` using the PO ID as the Kafka key.
- Successful publication marks the outbox event `PUBLISHED`.
- Kafka failures keep the event `PENDING`, record the error, increment the attempt count, and schedule exponential backoff.
- Events for the same Purchase Order are published in creation order so a later lifecycle event does not bypass an earlier pending event.
- Correlation ID, trace ID, actor identity and actor role are retained with the outbox record for troubleshooting and auditability.
- MongoDB transactions are enabled through `MongoTransactionManager`; MongoDB Atlas must support transactions (replica set/sharded deployment).

Outbox tuning:
- `OUTBOX_POLL_INTERVAL_MS` — default `1000`
- `OUTBOX_BATCH_SIZE` — default `50`
- `OUTBOX_INITIAL_BACKOFF_SECONDS` — default `2`
- `OUTBOX_MAX_BACKOFF_SECONDS` — default `300`

The service does not directly publish PO events from the business transaction anymore. The business transaction only persists the Purchase Order and its outbox event; the scheduled publisher handles Kafka delivery afterward.


## Phase 12 — Master Data Integration

The Purchase Order Service now resolves the supplier/vendor through the documented master-data API before creating a PO.

- Calls the documented Master Data endpoints to validate vendor, department and item references before PO creation.
- Forwards the authenticated JWT to the downstream service.
- Forwards `X-Correlation-Id` for distributed request tracing.
- Rejects missing vendors and inactive vendors.
- Uses bounded connection/read timeouts.
- Does not read the master-data MongoDB directly.
- PO service stores the vendor reference (`vendorId`) and keeps master-data ownership in `master-data-service`.
- Downstream availability failures return a stable integration error instead of silently creating a PO.

Configuration:

- `MASTER_DATA_SERVICE_BASE_URL` (default `http://master-data-service:8082/api/v1`)
- `MASTER_DATA_CONNECT_TIMEOUT_MS` (default `2000`)
- `MASTER_DATA_READ_TIMEOUT_MS` (default `5000`)


### DTO/package boundary

Transport DTOs are independent of the controller package and live under:

`com.recnaile.purchaseorder.dto`

The controller contains only HTTP transport logic. Application commands remain under
`com.recnaile.purchaseorder.application.command`, integration response models remain
under `com.recnaile.purchaseorder.client.masterdata`, and business services remain
under the single top-level `com.recnaile.purchaseorder.service` package.


## Phase 14 — Audit + Notification Integration Readiness

Audit and notification are separate microservices in the platform architecture. The Purchase Order Service does not duplicate their persistence or delivery responsibilities.

This phase therefore keeps the PO service responsible for publishing its documented PO lifecycle events through the transactional outbox. The existing `PO_CREATED` and `PO_ISSUED` events are the integration points for the future audit-service and notification-service. No private audit collection, audit API, notification database, or undocumented notification event is introduced here.

The dedicated `audit-service` and `notification-service` can consume the shared event contracts when those services are implemented. Inward/GRN integration remains deferred until the Inward Service and its contracts are implemented.


## Phase 15 — Observability

- Micrometer business counters for PO creation, lifecycle transitions, and outbox publish success/failure.
- Prometheus/Actuator metrics remain enabled.
- W3C trace propagation is explicitly configured.
- Correlation, trace, and span IDs are placed in MDC for structured logging.
- HTTP server request histogram/percentiles are enabled for operational latency visibility.
- No new business endpoints or service boundaries were introduced.


## Phase 16 — Testing + OpenAPI + Docker/K8s Readiness

- Added command validation tests for required create-PO fields.
- Added API contract smoke tests for the documented base path, query endpoints, and Idempotency-Key requirements on command endpoints.
- The service does not create a duplicate OpenAPI contract: the platform's centralized `contracts/openapi` remains the source of truth.
- The existing Docker image remains non-root, uses Java 21, exposes port 8084, and now installs only the minimal `wget` runtime dependency required by its liveness healthcheck.
- Graceful shutdown, Actuator health probes, environment-driven configuration, and the existing service Dockerfile remain compatible with the platform's Kubernetes/Helm deployment model.
- No Inward/GRN contract is invented; that integration remains deferred until `inward-service` and its shared contracts are implemented.


## Swagger / OpenAPI

When the service is running locally:

- Swagger UI: `http://localhost:8084/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8084/v3/api-docs`

The Swagger documentation includes JWT Bearer authentication, command idempotency requirements, and standard API error responses.

Detailed error documentation: `docs/swagger-api-error-documentation.md`.

### Current Purchase Order API field conventions

The REST API uses business-readable fields: `indentNumber`, `vendorName`, and `departmentName`.
PO line data is exposed under `items`, with each item containing exactly `itemId`, `itemName`,
`quantity`, `unitPrice`, and `gstRate`. The top-level `remarks` field is no longer part of the PO API.

## API semantics after the PO API alignment

- `POST /api/v1/purchase-orders` — direct/manual PO creation. Use only when procurement policy permits a PO to be raised without converting an approved indent.
- `POST /api/v1/purchase-orders/from-approved-indent` — normal approved-indent conversion. Send only `indentNumber`; PO Service reads the current indent from Indent Service and does not accept `indentStatus` from the client.
- `POST /api/v1/purchase-orders/{poId}/issue` — requires `{ "reason": "..." }`.
- `POST /api/v1/purchase-orders/{poId}/cancel` — requires `{ "reason": "..." }`.
- PO identifiers are generated by the backend as `PO%2F26-27%2F001`, `PO%2F26-27%2F002`, ... .
- `vendorName` is validated against active vendors in Master Data Service.

## PO ID migration note

The public `poNumber` field has been removed. The backend-generated `poId` is now the single Purchase Order identifier (`PO%2F26-27%2F001`, `PO%2F26-27%2F002`, ...).

If an existing MongoDB `purchase_orders` collection still has the old unique `poNumber` index, drop that obsolete index before creating new POs. The exact index name should be checked with `db.purchase_orders.getIndexes()`; older deployments commonly created it as `poNumber`.

Example MongoDB shell command after verifying the index name:

`db.purchase_orders.dropIndex("poNumber")`

## Latest stability fixes

- PurchaseOrderResponse is null-safe for legacy MongoDB PO documents where `lines` or `sourceIndentIds` may be null.
- PurchaseOrderEventPayload is null-safe for the same collection fields.
- PurchaseOrder collection setters normalize null lists to empty lists.
- GlobalExceptionHandler now logs the actual unhandled exception and request URI while preserving the stable INTERNAL_ERROR response contract.
- Added regression coverage for mapping legacy PO documents with null collections.
