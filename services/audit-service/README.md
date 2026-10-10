# RECNAILE Audit Service

This module belongs at `services/audit-service` in the existing RECNAILE Gradle multi-project repository. This ZIP contains exactly one Markdown file (`README.md`); the producer integration notes are plain text, not additional README or phase Markdown files.

## Phase status

- **Phases 01–07:** Domain model, MongoDB append-only persistence, Kafka consumption, duplicate-event handling, application services, read-only APIs, and JWT authorization aligned to the Auth Service's HS256 token implementation.
- **Phase 08 package:** Adds an event-schema reference and transactional-outbox producer integration template.
- **Phase 09:** Adds SHA-256 integrity digests, an MD-only integrity verification endpoint, and a non-destructive retention preview. Existing records without a digest are reported as legacy/unverifiable; no records are automatically deleted. Duplicate event IDs remain idempotent only when content matches exactly.

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
- `GET /api/v1/audit-events/operations/integrity/{eventId}` verifies one event. MD role required.
- `GET /api/v1/audit-events/operations/retention-preview` reports eligible records only. MD role required; it never deletes or modifies records.
- `AUDIT_RETENTION_DAYS` defaults to `0` (disabled). Set a positive value only after the platform retention policy is approved. No TTL index or destructive deletion job is created.
- Kafka redelivery is controlled through the existing stable event ID and exact-content duplicate detection. A conflicting event ID is rejected, not overwritten.


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
- Retention preview: `AUDIT_RETENTION_DAYS` (default `0`, disabled).

## Replace module (PowerShell)

Run from repository root. Back up or commit current work before replacing the module.

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-09-startup-fix-v2.zip -DestinationPath .\services -Force
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

**Summary:** Added event integrity digests and MD-only verification, a non-destructive retention preview, and explicit replay/duplicate safeguards. Preserved HS256 Auth Service compatibility, append-only persistence, and exactly one README. Producer integration remains a template until applied to each business service in the complete repository.

```powershell
git add services/audit-service
git commit -m "feat(audit-service): add integrity and retention controls"
```

## Verification status

The Spring startup failure reported for `AuditRetentionService` is fixed by explicitly marking its production constructor with `@Autowired`; the package-private clock constructor remains available to unit tests. ZIP structure, Java source patch, YAML duplicate-key count, and schema JSON syntax were checked during packaging. Gradle compilation/tests and Docker startup were not executable in this packaging environment; run both Gradle commands and verify the container health in the full repository before deploying. Integrity digests detect changes to stored fields but are not a substitute for externally protected signatures or immutable storage. The retention endpoint is preview-only and does not delete records.


## Phase 9 compilation fix

Removed duplicate declarations introduced in the previous archive: duplicated controller service fields/imports, duplicated Mongo repository method/import, and duplicated `requireMdOperations` policy method.
