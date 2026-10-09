# Notification Service

Event-driven, in-app notification service for the RECNAILE Procurement Platform.

- **Port:** `8090`
- **MongoDB database:** `notification`
- **Java:** `21`
- **Kafka consumer group:** `notification-service`
- **Canonical recipient identity:** `empId` only
- **Email delivery:** not implemented / out of scope
- **Audit persistence and replay audit trail:** owned by the separate Audit Service

## API error documentation

Swagger UI includes a shared HTTP error catalogue on each operation, plus endpoint-specific
responses for notification inbox and audited replay APIs. Documented statuses include:

| Status | Meaning |
|---|---|
| `400` | Invalid request parameter, path value, or rejected operation input |
| `401` | Missing/invalid/expired JWT, invalid issuer/signature, or missing `employeeId` claim |
| `403` | Authenticated caller lacks the required authority |
| `404` | Notification/event not found or unavailable to the caller |
| `405` | HTTP method not supported for the path |
| `409` | Conflict with resource or processing state, where applicable |
| `413` / `415` | Request body too large / unsupported content type, where applicable |
| `422` | Semantic validation failure, where applicable |
| `429` | Configured rate limit exceeded, where applicable |
| `500` | Unexpected application failure |
| `502` / `503` | Upstream failure or temporarily unavailable dependency, where applicable |

These are OpenAPI descriptions, not a promise that every status is emitted by every endpoint.
Error response bodies can vary across Spring Security, validation, application, and infrastructure
failures. This documentation change does not introduce a custom global exception handler or
standardize runtime error JSON.

## Swagger UI / OpenAPI

After starting the service, open:

- Swagger UI: `http://localhost:8090/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8090/v3/api-docs`

The Swagger UI and OpenAPI document endpoints are public so the documentation can load.
Business API endpoints remain protected by JWT. In Swagger UI, select **Authorize** and
enter a valid Auth Service access token (paste the token; do not include `Bearer ` unless
the UI explicitly asks for it). The service validates the token signature, issuer, and
the required `employeeId` claim.

Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9`.

## Phase 11 baseline retained

- Durable Kafka event inbox and processing ledger.
- Idempotent notification persistence with MongoDB uniqueness constraints.
- Notification templates and in-app draft orchestration.
- Authenticated-user inbox endpoints, unread count, read/read-all, delete-own-inbox, and server-side pagination.
- MongoDB startup migration from legacy `recipientId` to canonical `empId`, with a fail-fast guard if a document has conflicting identities.
- Inbox queries and mutations are scoped to the authenticated employee identity.

Event payload recipient resolution accepts only `empId` and `recipients: [{"empId":"EMP-001"}]`. It does not fall back to `userId`, `employeeId`, `requestedBy`, `createdBy`, or `recipientId`.

## Phase 12 — JWT authentication and API security

- Adds Spring Security and OAuth2 Resource Server dependencies.
- All endpoints require a valid signed JWT except `/actuator/health` and its subpaths, which are public for container liveness/readiness checks.
- Stateless sessions and CSRF disabled for this stateless bearer-token API.
- Matches the uploaded Auth Service implementation: HS256, `JWT_SECRET_KEY` interpreted as UTF-8 bytes, and issuer `auth-service` by default.
- Auth Service issues the employee identity claim as `employeeId` (not `empId`); Notification Service validates that claim and maps its verified value to the internal canonical recipient identity `empId`.
- Tokens with missing/blank `employeeId`, invalid signatures, wrong issuer, or invalid timestamps are rejected. The MongoDB subject (`sub`) is never used as an identity fallback.
- The authenticated principal name is the verified `employeeId`, so inbox operations remain employee-scoped and clients cannot choose another employee through a request parameter.
- No email delivery or audit-service responsibility is added in this phase.

### Required deployment settings

The uploaded Compose configuration already applies `env_file: [.env]` to both Auth Service and Notification Service, and the provided `.env` contains `JWT_SECRET_KEY`. No JWKS endpoint is used: the uploaded Auth Service signs HS256 tokens with the UTF-8 bytes of that same secret and defaults the issuer to `auth-service`.

Keep `JWT_SECRET_KEY` identical in both services and set `JWT_ISSUER` only if you intentionally configure a different issuer. Do not rotate the key casually in a running environment because existing access tokens and other services using the shared key will stop validating.

Example PowerShell check of the environment configured for Compose:

```powershell
docker compose config
```

Do not commit the shared secret or log bearer tokens. The Notification Service reads the shared secret from the local `.env` at runtime.

## API security behavior

| Request | Access |
|---|---|
| `GET /actuator/health` and `/actuator/health/**` | Public health probes |
| `/api/v1/notifications/**` | Valid JWT with non-empty `employeeId` claim (mapped internally to `empId`) |
| Other endpoints, including metrics | Authenticated JWT required |

The inbox API derives identity from the authenticated JWT principal. It never accepts an `empId` supplied by the caller to select a different inbox.

## Configuration

- `MONGODB_BASE_URI`, `MONGODB_DATABASE`
- `KAFKA_BOOTSTRAP_SERVERS`, topic environment variables, `KAFKA_CONSUMER_GROUP`
- `JWT_SECRET_KEY` — required shared secret already supplied through `.env`; same value and interpretation as Auth Service\n- `JWT_ISSUER` — optional; defaults to `auth-service` to match Auth Service
- `OTEL_EXPORTER_OTLP_ENDPOINT`, `TRACING_SAMPLE_RATE`

Do not commit credentials or log bearer tokens, JWT claims beyond what is needed for diagnostics, or raw event payloads.

## Validation (PowerShell, from repository root)

Back up the existing service folder before replacing it. Extract this archive, then copy its `notification-service` directory into `services/notification-service`.

```powershell
# Destructive: run only after backing up the current service folder.
Remove-Item -Recurse -Force .\services\notification-service

.\gradlew.bat :services:notification-service:test
.\gradlew.bat :services:notification-service:build
docker compose build notification-service
docker compose up -d notification-service
docker compose ps notification-service
docker compose logs --tail=100 notification-service
```

The test suite includes checks that the Auth Service `employeeId` claim becomes the canonical recipient principal and that tokens without that claim are rejected. Run the Gradle commands in the full repository; this service-only workspace does not include the root Gradle wrapper or all sibling modules, so a full-repository build has not been claimed here.

## Git commit

```powershell
git add services/notification-service
git commit -m "feat(notification): enforce JWT authentication and empId principal"
```

## Phases 13–17 consolidated hardening

See [`PHASES-01-17.md`](PHASES-01-17.md) for the full phase map, retry/DLT configuration, audited replay endpoint, and production acceptance checklist.

- Kafka retries use a bounded backoff and route exhausted records to the configured `.dlt` topic suffix.
- Audited replay endpoint: `POST /api/v1/notifications/operations/events/{eventId}/replay`. It requires `ROLE_ADMIN`, `ROLE_AUDIT_ADMIN`, or `SCOPE_notification.replay` and publishes to `KAFKA_AUDIT_TOPIC` before reprocessing the durable inbox event.
- The separate Audit Service must be enabled and consume the audit topic to persist replay audit records. The Notification Service does not own audit persistence.
- Delivery remains in-app only; no email transport is implemented.
- Phase 17 is not considered verified until the full-repository test suite and deployment acceptance checklist pass.

## Notification replay authorization (RBAC)

The event replay endpoint
`POST /api/v1/notifications/operations/events/{eventId}/replay` is restricted to the
Managing Director authority `ROLE_MD`. Other roles are denied unless the authorization
policy is deliberately changed. The Auth Service must issue/map the MD role to the exact
Spring Security authority `ROLE_MD`; verify this mapping with a real MD JWT before release.

The `/me` inbox endpoints always operate on the authenticated employee's own notifications,
including for MD. The MD role does not make `/me` endpoints cross-employee endpoints.

