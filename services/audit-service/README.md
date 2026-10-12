# RECNAILE Audit Service

This module belongs at `services/audit-service` in the existing RECNAILE Gradle multi-project repository. This ZIP contains exactly one Markdown file (`README.md`); the producer integration notes are plain text, not additional README or phase Markdown files.

## Phase status

- **Phases 01–07:** Domain model, MongoDB append-only persistence, Kafka consumption, duplicate-event handling, application services, read-only APIs, and JWT authorization aligned to the Auth Service's HS256 token implementation.
- **Phase 08 package:** Adds an event-schema reference and transactional-outbox producer integration template.
- **Phase 09:** Adds SHA-256 digests to persisted audit records and a non-destructive retention preview. No public integrity-verification API is exposed, and no records are automatically deleted. Duplicate event IDs remain idempotent only when content matches exactly.
- **Phase 10:** Hardens query performance and API contract coverage. Correlation/trace history has compound indexes aligned with descending timestamp queries, with regression tests for index definitions and OpenAPI coverage for service/action and retention-preview endpoints.
- **Phase 11:** Centralizes actor identity resolution. The validated Auth Service JWT's `employeeId` is the preferred business actor identity; `sub` is used only as a compatibility fallback for older tokens. Regression tests cover both claims and missing identity. This is identity resolution for audit access, not notification-recipient delivery.
- **Phase 12:** Hardens authorization regression coverage for employee self-history, global PROCUREMENT/MD access, MD-only operations, `ROLE_`-prefixed role claims, missing claims, and identity-claim precedence. The existing stateless HS256 resource-server validation, issuer validation, and JSON 401/403 handlers are preserved.
- **Phase 13:** Configurable Kafka retry policy and dead-letter routing.
- **Phase 14:** MD-only replay endpoint for a caller-supplied failed event envelope.
- **Phase 15:** Micrometer ingestion outcome counters.
- **Phase 16:** OpenAPI documentation and observability hardening.
- **Phase 17:** Production verification checklist and final integration guidance.

**Important scope note:** The available files do not contain the complete current source for every business service. Therefore this ZIP does not claim that producers have already been changed in all business services. Apply the template to each producer using that service's existing outbox implementation and reconcile the schema with the authoritative root `contracts/events/AUDIT_EVENT.schema.json` before rollout. This avoids inventing producer code or breaking service-specific transaction boundaries.

## Event flow

1. A business service commits its business change and outbox row in one local transaction.
2. Its outbox relay publishes the event to `procurement.audit.events`, using `eventId` as the Kafka key.
3. Audit Service validates the event, performs idempotent append-only MongoDB persistence, and acknowledges the Kafka offset only after persistence succeeds.
4. Exhausted consumer failures are routed to `procurement.audit.events.dlt` according to the configured retry policy.

Never access another service's database directly. Do not include credentials, JWTs, signing secrets, or unnecessary personal data in audit payloads.

## Canonical event fields

The event model includes `eventId`, `eventType` (`AUDIT_EVENT`), `eventVersion`, `occurredAt`, `producer`, optional `correlationId` and `traceId`, and a payload with `actorId`, `actorRoles`, `service`, `entityType`, `entityId`, `action`, `timestamp`, and supported optional business references/statuses. See `src/main/resources/contracts/audit-event.schema.json`. The root repository's canonical event schema remains authoritative.

## Auth Service compatibility

Audit Service validates HS256 tokens using `JWT_SECRET_KEY` as UTF-8 bytes and `JWT_ISSUER` (default `auth-service`). Role mapping reads the validated `roles` claim. Do not configure RS256/JWK Set validation for this Auth Service unless the Auth Service is deliberately changed as a coordinated platform migration.

## Phase 09 integrity and retention controls

- New audit events receive a SHA-256 digest over persisted event fields. The digest is intended to detect accidental or unauthorized field changes; it is not a digital signature and does not protect against an attacker who can rewrite both fields and digest.
- `GET /api/v1/audit-events/operations/retention-preview` reports eligible records only. MD role required; it never deletes or modifies records.
- `AUDIT_RETENTION_DAYS` defaults to `0` (disabled). Set a positive value only after the platform retention policy is approved. No TTL index or destructive deletion job is created.
- Kafka redelivery is controlled through the existing stable event ID and exact-content duplicate detection. A conflicting event ID is rejected, not overwritten.



## Swagger UI and OpenAPI

When the service is running locally on its default port, open:

- **Swagger UI:** `http://localhost:8091/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8091/v3/api-docs`
- **OpenAPI YAML:** `http://localhost:8091/v3/api-docs.yaml`

Swagger UI is provided by `springdoc-openapi-starter-webmvc-ui`. The documentation endpoints are public so developers can inspect the contract; the actual audit APIs remain protected by JWT and role/entity authorization. In Swagger UI, use **Authorize** and enter `Bearer <access-token>` (or the token in the format requested by the UI) before trying protected endpoints. Do not paste tokens into screenshots or commit them.

## API error response contract

Errors use the standard envelope:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "FORBIDDEN",
    "message": "You are not authorized to access this audit data",
    "traceId": null
  }
}
```

Common HTTP outcomes documented in Swagger UI:

- `400 VALIDATION_ERROR`: invalid UUID, pagination values, request JSON, or request validation.
- `401 UNAUTHENTICATED`: bearer token is missing or invalid (including issuer/signature validation).
- `403 FORBIDDEN`: token is valid but its role or data scope does not permit the operation.
- `404 NOT_FOUND`: the requested audit event does not exist, where applicable.
- `500 INTERNAL_ERROR`: unexpected server error; the response avoids exposing internal exception details. Use `traceId` when available to correlate logs.

Not every status applies to every endpoint; consult each endpoint’s response documentation in Swagger UI.

## Configuration

- HTTP port: `SERVICE_PORT` (default `8091`).
- Mongo base URI: `MONGODB_BASE_URI`; database: `MONGODB_DATABASE` (default `audit`).
- Kafka bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS` (default `kafka:29092`).
- Input topic: `KAFKA_AUDIT_TOPIC` (default `procurement.audit.events`).
- Consumer group: `KAFKA_CONSUMER_GROUP` (default `audit-service.audit-events`).
- Dead-letter topic: `<input-topic>.dlt`.
- JWT signing secret: `JWT_SECRET_KEY` (required; do not commit `.env`).
- JWT issuer: `JWT_ISSUER` (default `auth-service`).
- Retention preview: `AUDIT_RETENTION_DAYS` (default `0`, disabled).

## Replace module (PowerShell)

Run from repository root. Back up or commit current work before replacing the module.

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-17-no-integrity-api.zip -DestinationPath .\services -Force
```

## Test and build

```powershell
.\gradlew.bat :services:audit-service:test
.\gradlew.bat :services:audit-service:build
```

## Docker Compose

```powershell
docker compose up -d --build audit-service
```

Ensure the Audit Service block is enabled in root Compose and receives `JWT_SECRET_KEY`, `JWT_ISSUER`, `MONGODB_BASE_URI`, and `KAFKA_BOOTSTRAP_SERVERS`. This module ZIP does not replace root Compose or modify business-service producer code.

## Git commit summary

**Summary:** Complete Audit Service update through Phase 17 with identity and authorization hardening, configurable Kafka retries/DLT, MD-only replay, ingestion metrics, Swagger/OpenAPI documentation, API error response documentation, retention safeguards, HS256 Auth Service compatibility, and append-only persistence. The integrity-verification HTTP endpoint has been removed. Producer integration remains a template until applied to each business service in the complete repository.

```powershell
git add services/audit-service
git commit -m "feat(audit-service): remove integrity endpoint"
```

## Verification status

The Phase 9 startup fix is preserved: `AuditRetentionService` uses an explicitly marked production constructor and retains its package-private clock constructor for unit tests. Packaging checks include ZIP integrity, exactly one README, JSON syntax, YAML parsing, and presence of Phase 10 indexes plus Phase 11/12 identity and authorization tests. Gradle compilation/tests and Docker startup could not be executed in this packaging environment; run the supplied Gradle commands in the full repository before deployment. Do not treat static packaging checks as proof that tests pass. Persisted SHA-256 digests are internal metadata and no integrity-verification API is exposed. Retention remains preview-only and does not delete records.


## Phase 9 compilation fix

Removed duplicate declarations introduced in the previous archive: duplicated controller service fields/imports, duplicated Mongo repository method/import, and duplicated `requireMdOperations` policy method.
