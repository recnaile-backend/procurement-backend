# RECNAILE Audit Service

This module is intended for `services/audit-service` in the existing RECNAILE Gradle multi-project repository. Keep the repository root Gradle settings/build files, shared contracts, and root Docker Compose configuration from the full repository. This module ZIP intentionally contains exactly one Markdown file: `README.md`.

## Implemented phases

- **Phase 01 — Domain model:** Typed `AUDIT_EVENT` envelope and explicit payload fields with contract tests.
- **Phase 02 — MongoDB persistence:** Append-only event documents, repository access, and history indexes.
- **Phase 03 — Kafka consumer:** JSON parsing, Bean Validation, manual acknowledgement after persistence, bounded retries, and dead-letter publishing.
- **Phase 04 — Idempotency:** Event UUID is the durable identity; exact duplicates are accepted and conflicting reuse is rejected.
- **Phase 05 — Application services:** Separate ingestion and read-only query use cases; Kafka adapter depends on the ingestion service, and query logic is isolated from persistence details. Removed the generic mutable CRUD controller so audit records are not exposed through update/delete operations.

## Configuration

- Port: `SERVICE_PORT` (default `8091`).
- MongoDB base URI: `MONGODB_BASE_URI`; database: `MONGODB_DATABASE` (default `audit`).
- Kafka bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS` (default `kafka:29092`).
- Input topic: `KAFKA_AUDIT_TOPIC` (default `procurement.audit.events`).
- Consumer group: `KAFKA_CONSUMER_GROUP` (default `audit-service.audit-events`).
- Dead-letter topic: `<input-topic>.dlt`.

## Replace the module (PowerShell)

Run from the repository root. Keep a backup or commit your current work before replacing the directory.

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-05.zip -DestinationPath .\services -Force
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

Confirm the Audit Service block is enabled in the root Compose file and that the Kafka input and DLT topics exist. This module ZIP does not replace or modify the repository's root Compose file.

## Git commit summary

**Summary:** Separated Audit Service ingestion and query use cases into application services, routed Kafka consumption through the ingestion service, removed the legacy mutable CRUD API, and added application-service tests. Kept a single README file.

```powershell
git add services/audit-service
git commit -m "refactor(audit-service): separate ingestion and query services"
```

## Important behavior

- Audit events are append-only; no update/delete application use case is exposed.
- MongoDB insertion must succeed before Kafka acknowledgement.
- Exact redelivery is idempotent; same event ID with different content is rejected.
- Query services are internal application use cases in this phase; REST query endpoints and authorization are a later phase.
- Run Gradle tests and build in the complete repository before deploying. Successful ZIP integrity checks do not prove compilation or runtime behavior.
