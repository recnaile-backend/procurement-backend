# Audit Service

RECNAILE Audit Service module. This ZIP contains the module folder intended to be placed at `services/audit-service` in the existing Gradle multi-project repository. Keep the repository root `settings.gradle`, root `build.gradle`, version catalog/conventions, contracts, and Compose file from the full repository.

## Phase status
- Phase 01: typed `AUDIT_EVENT` domain envelope and payload.
- Phase 02: MongoDB persistence document, append-only insert service, repository query methods, and documented indexes.
- Phase 03: Kafka consumer for the canonical `AUDIT_EVENT` envelope, validation, manual acknowledgement after persistence, bounded retries, and `.dlt` publishing.
- Phase 04: idempotent duplicate handling using MongoDB event identity; exact replays are accepted, conflicting reuse of an event ID is rejected.

## Configuration
- Service port: `SERVICE_PORT` (default `8091`).
- Mongo URI base: `MONGODB_BASE_URI`; database name: `MONGODB_DATABASE` (default `audit`).
- Kafka topic defaults to `procurement.audit.events`; consumer group defaults to `audit-service.audit-events`. Configure `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_AUDIT_TOPIC`, and `KAFKA_CONSUMER_GROUP` for the target environment.

## Commands (PowerShell, from repository root)
Delete existing module before replacing it with the ZIP contents:

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-04.zip -DestinationPath .\services -Force
```

Run module tests and build using the repository wrapper:

```powershell
.\gradlew.bat :services:audit-service:test
.\gradlew.bat :services:audit-service:build
```

Start the Compose stack:

```powershell
docker compose up --build -d
```

**Compose note:** if the audit-service block is still commented out in your root Compose file, Docker Compose will not start it until that block is enabled and its environment/network settings are checked. This ZIP does not silently rewrite the root Compose file. Ensure `procurement.audit.events` and its `.dlt` topic are provisioned. The consumer retries failed processing and sends exhausted failures to the `.dlt` topic.

## Git summary
```powershell
git add services/audit-service
git commit -m "feat(audit-service): handle duplicate audit events idempotently"
```


## Phase 04 — Idempotent event processing

- MongoDB's unique event ID (`_id`) is the durable idempotency key.
- A replay with the same ID and identical stored content is treated as already processed; the Kafka listener can safely acknowledge it.
- Reuse of an event ID with different content raises `AuditEventIdentityConflictException`; it is not silently accepted and is eligible for the configured retry/DLT policy.
- Audit records remain append-only: no `save()` or update path is used.
- A crash after Mongo insert but before Kafka acknowledgement is safe: redelivery is recognized as an exact duplicate.

See `PHASE-04-IDEMPOTENCY.md` for behavior and verification steps.
