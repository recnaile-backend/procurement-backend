# RECNAILE Audit Service

This module belongs at `services/audit-service` in the existing RECNAILE Gradle multi-project repository. Preserve root Gradle settings/build files, shared contracts, and root Docker Compose configuration. This ZIP contains exactly one Markdown file: `README.md`; no separate phase notes are included.

## Implemented phases

- **Phase 01 — Domain model:** Typed `AUDIT_EVENT` envelope and explicit payload fields with contract tests.
- **Phase 02 — MongoDB persistence:** Append-only event documents, repository access, and history indexes.
- **Phase 03 — Kafka consumer:** JSON parsing, validation, manual acknowledgement after persistence, bounded retries, and dead-letter publishing.
- **Phase 04 — Idempotency:** Event UUID is durable identity; exact duplicates are accepted and conflicting reuse is rejected.
- **Phase 05 — Application services:** Separate ingestion and read-only query use cases.
- **Phase 06 — Read-only REST API:** Versioned query endpoints, bounded pagination, standardized envelopes, and OpenAPI.
- **Phase 07 — Security:** Stateless OAuth2 Resource Server, HS256 validation matching the Auth Service `JwtKeyConfig`, issuer validation, `roles` claim mapping, 401/403 envelopes, and actor-scope authorization using the `employeeId` claim (with `sub` fallback).

## Required security environment

The root Compose service uses `env_file: [.env]`, so Audit Service receives `JWT_SECRET_KEY` and `JWT_ISSUER` from the root environment file. Do not hardcode or commit the signing secret. The key must be at least 32 UTF-8 bytes, matching Auth Service validation.

## API endpoints

Base path: `/api/v1/audit-events`. All endpoints require a valid bearer JWT.

| Method | Endpoint | Access policy |
|---|---|---|
| GET | `/{eventId}` | PROCUREMENT/MD can read any event; EMPLOYEE/TL can read only events whose `actorId` matches JWT `employeeId` (fallback `sub`) |
| GET | `/actors/{actorId}?page=0&size=20` | PROCUREMENT/MD can read any actor; EMPLOYEE/TL only their own `employeeId` (fallback `sub`) |
| GET | `/entities/{entityType}/{entityId}?page=0&size=20` | PROCUREMENT/MD only |
| GET | `/correlations/{correlationId}?page=0&size=20` | PROCUREMENT/MD only |
| GET | `/traces/{traceId}?page=0&size=20` | PROCUREMENT/MD only |
| GET | `/services/{service}/actions/{action}?page=0&size=20` | PROCUREMENT/MD only |

Auth Service reference checked: `JwtService` issues **HS256** tokens, `iss` defaults to `auth-service`, `sub` is the MongoDB user ID, `employeeId` is the business identity, and `roles` contains values such as `EMPLOYEE`, `PROCUREMENT`, `TL`, and `MD`. `JwtKeyConfig` uses the `JWT_SECRET_KEY` value as UTF-8 bytes (not Base64-decoded); Audit Service uses the same environment variable and issuer default (`JWT_ISSUER`, default `auth-service`). Role values are read from the validated JWT `roles` claim and mapped to `ROLE_*`. The supported platform roles are `EMPLOYEE`, `PROCUREMENT`, `TL`, and `MD`; no `ADMIN` role is granted. Caller-supplied identity headers are never trusted.

**Scope limitation:** The current canonical audit event payload has `actorId` but no department/owner field. Therefore this phase enforces self-only access for EMPLOYEE/TL where actor ownership is verifiable and restricts cross-actor/entity-wide searches to PROCUREMENT/MD. It does not claim department-level scoping. To grant TL department-scoped audit search, the canonical event contract must first provide a trustworthy department/entity-scope field and the authorization rule must be agreed.

## JWT configuration (required)

Set the Auth Service JWK Set URI and issuer for each environment:

- `JWT_SECRET_KEY` — required URL of the Auth Service's public JWK Set. No guessed default is provided; the service fails to initialize security if this value is missing.
- `JWT_ISSUER` — expected issuer, defaults to `auth-service` to match the platform token contract.

The decoder explicitly accepts RS256 and validates issuer, expiration, and not-before claims. The JWK endpoint path must match the actual Auth Service deployment. Do not point it at a token endpoint or a private-key endpoint.

Health probes (`/actuator/health/**`) and API documentation (`/v3/api-docs/**`, `/swagger-ui/**`) are public; all audit query routes require authentication and authorization. Return codes use the platform envelope: 401 `UNAUTHENTICATED`, 403 `FORBIDDEN`, 404 `NOT_FOUND`.

## OpenAPI

- Runtime OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`
- Source contract: `src/main/resources/openapi/audit-service.openapi.yaml`

## Configuration

- Port: `SERVICE_PORT` (default `8091`).
- MongoDB base URI: `MONGODB_BASE_URI`; database: `MONGODB_DATABASE` (default `audit`).
- Kafka bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS` (default `kafka:29092`).
- Input topic: `KAFKA_AUDIT_TOPIC` (default `procurement.audit.events`).
- Consumer group: `KAFKA_CONSUMER_GROUP` (default `audit-service.audit-events`).
- Dead-letter topic: `<input-topic>.dlt`.

## Replace the module (PowerShell)

Run from the repository root. Commit or back up current work before replacing the directory.

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-07.zip -DestinationPath .\services -Force
```

## Verify tests and build

```powershell
.\gradlew.bat :services:audit-service:test
.\gradlew.bat :services:audit-service:build
```

## Start Docker Compose

```powershell
docker compose up --build -d
```

Confirm the Audit Service block is enabled in the root Compose file, set `JWT_SECRET_KEY` and `JWT_ISSUER`, and ensure Kafka input and DLT topics exist. This module ZIP does not replace or modify root Compose configuration.

## Git commit summary

**Summary:** Added stateless RS256 JWT resource-server security with issuer validation, roles claim mapping, standardized 401/403 responses, and authorization rules that limit EMPLOYEE/TL to their own actor history/events while reserving global audit searches for PROCUREMENT/MD. Added access-policy tests and maintained append-only history and one README file.

```powershell
git add services/audit-service
git commit -m "fix(audit-service): align JWT validation with auth service"
```

## Important behavior

- Audit events are append-only; no update/delete endpoint is exposed.
- MongoDB insertion must succeed before Kafka acknowledgement.
- Exact redelivery is idempotent; conflicting reuse of an event ID is rejected.
- ZIP integrity and source inspection do not prove compilation. Run the Gradle test/build commands in the full repository before proceeding.
