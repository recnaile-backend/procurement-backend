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
