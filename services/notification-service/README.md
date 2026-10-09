# Notification Service

Event-driven, in-app notification service for the RECNAILE Procurement Platform.

- **Port:** `8090`
- **MongoDB database:** `notification`
- **Java:** `21`
- **Kafka consumer group:** `notification-service`
- **Canonical recipient identity:** `empId` only
- **Email delivery:** not implemented / out of scope
- **Audit persistence and replay audit trail:** owned by the separate Audit Service

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
