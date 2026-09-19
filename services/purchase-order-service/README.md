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

MongoDB indexes are defined for unique PO number, source indent lookup, vendor/status,
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

PO numbers are generated server-side in the documented format `PO-YYYY-0001`. MongoDB atomically increments one sequence document per year, and `purchase_orders.poNumber` remains uniquely indexed. The generator is invoked during PO creation; clients cannot supply or override the PO number.

## Phase 7 REST APIs

Base path: `/api/v1/purchase-orders`

- `POST /from-approved-indent` — create a PO from a validated READY_FOR_PO indent snapshot.
- `GET /{poId}` — get a PO by ID.
- `GET /` — list POs with optional `status`, `vendorId`, and `projectId` filters.
- `POST /{poId}/issue` — transition DRAFT -> ISSUED.
- `POST /{poId}/cancel` — transition to CANCELLED where allowed.
- `POST /{poId}/complete` — transition to COMPLETED where allowed.
- `POST /{poId}/close` — transition COMPLETED -> CLOSED.

Authentication/RBAC is intentionally handled in Phase 8; this phase does not introduce raw identity headers.

## Phase 8 — Security & RBAC

The service is a stateless OAuth2 Resource Server using JWT/RS256. JWT roles are
read from the `roles` claim and normalized to Spring `ROLE_*` authorities.
The documented roles are Employee, Procurement, TL and MD.

Purchase-order access follows the documented module responsibilities:
- Employee: no PO API access.
- Procurement: create, view, issue/send, cancel, complete and close POs.
- TL: view POs.
- MD: view POs.
- MD: full PO access.

Configure:
- `APP_SECURITY_JWT_JWK_SET_URI` — Auth Service JWKS endpoint used to validate RS256 signatures.
- `APP_SECURITY_JWT_ISSUER_URI` — optional issuer validation value.

Only Actuator health/info are public. All PO APIs require a valid Bearer token.
Raw identity headers are not trusted; the authenticated JWT subject is used as the actor ID.
Entity-level authorization and retry/idempotency controls are strengthened in later phases.


## Phase 9 — Idempotency & Optimistic Concurrency

Implemented:
- `Idempotency-Key` is required on all retry-safe PO POST commands.
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

### Required headers
All command POSTs require:
`Idempotency-Key: <unique-client-key>`

### Mongo index note
`uk_source_indent` is a unique multikey index. If an older non-unique `idx_source_indent` index already exists in an existing database, drop that obsolete index before enabling automatic index creation for the new unique index.

## Phase 10 — Kafka Events

The Purchase Order Service defines the documented purchase-order domain events:

- `PO_CREATED` after a Purchase Order is successfully persisted.
- `PO_ISSUED` after a Purchase Order transitions to `ISSUED` and is persisted.
- `PO_CLOSED` after a Purchase Order transitions to `CLOSED` and is persisted.

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
- `PO_CREATED`, `PO_ISSUED`, and `PO_CLOSED` are stored as `PENDING` outbox events before Kafka publication.
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
