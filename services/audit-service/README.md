# Audit Service

Spring Boot service module for the RECNAILE Procurement Platform. Java 21; Spring Boot version is managed by the repository root build.

## Phase 01

See [PHASE-01-DOMAIN-MODEL.md](PHASE-01-DOMAIN-MODEL.md) for the typed audit event model and verification steps. The canonical event contracts are maintained in the repository root under `contracts/events/`.

## Commands (run from repository root)

```powershell
# Delete the existing service folder before extracting the updated service ZIP
Remove-Item -Recurse -Force .\services\audit-service

# Run service tests
.\gradlew.bat :services:audit-service:test

# Build service
.\gradlew.bat :services:audit-service:build

# Start the configured platform stack (requires the root compose file and environment configuration)
docker compose up --build -d
```

This is the Phase 01 domain-model update only. Kafka ingestion, persistence, idempotency, API redesign and RBAC remain for subsequent phases. Do not interpret the legacy CRUD API as a production-ready audit interface.
