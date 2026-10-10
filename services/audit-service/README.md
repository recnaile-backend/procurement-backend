# RECNAILE Audit Service

This module belongs at `services/audit-service` in the existing RECNAILE Gradle multi-project repository. This ZIP contains exactly one Markdown file (`README.md`); the producer integration notes are plain text, not additional README or phase Markdown files.

## Phase status

- **Phases 01–07:** Domain model, MongoDB append-only persistence, Kafka consumption, duplicate-event handling, application services, read-only APIs, and JWT authorization aligned to the Auth Service's HS256 token implementation.
- **Phase 08 package:** Adds a versioned JSON Schema reference for the `AUDIT_EVENT` envelope and a producer integration template covering transactional-outbox publishing, stable event IDs, Kafka acknowledgement, correlation/trace propagation, and sensitive-data restrictions.

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

## Configuration

- HTTP port: `SERVICE_PORT` (default `8091`).
- Mongo base URI: `MONGODB_BASE_URI`; database: `MONGODB_DATABASE` (default `audit`).
- Kafka bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS` (default `kafka:29092`).
- Input topic: `KAFKA_AUDIT_TOPIC` (default `procurement.audit.events`).
- Consumer group: `KAFKA_CONSUMER_GROUP` (default `audit-service.audit-events`).
- Dead-letter topic: `<input-topic>.dlt`.
- JWT signing secret: `JWT_SECRET_KEY` (required; do not commit `.env`).
- JWT issuer: `JWT_ISSUER` (default `auth-service`).

## Replace module (PowerShell)

Run from repository root. Back up or commit current work before replacing the module.

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-08.zip -DestinationPath .\services -Force
```

## Test and build

```powershell
.\gradlew.bat :services:audit-service:test
.\gradlew.bat :services:audit-service:build
```

## Docker Compose

```powershell
docker compose up --build -d
```

Ensure the Audit Service block is enabled in root Compose and receives `JWT_SECRET_KEY`, `JWT_ISSUER`, `MONGODB_BASE_URI`, and `KAFKA_BOOTSTRAP_SERVERS`. This module ZIP does not replace root Compose or modify business-service producer code.

## Git commit summary

**Summary:** Prepared Phase 08 audit-event producer integration support with an explicit event schema reference and transactional-outbox integration template. Preserved existing Audit Service consumer behavior, HS256 Auth Service compatibility, append-only persistence, and exactly one README. Business-service producer changes must be applied and tested against the current full repository source.

```powershell
git add services/audit-service
git commit -m "feat(audit-service): prepare phase 8 event producer integration"
```

## Verification status

ZIP integrity and schema JSON syntax can be checked locally. Gradle compilation/tests have not been executed in this environment. Run both commands above in the full repository before deploying. The integration template is guidance, not proof that every business-service producer has been implemented.
