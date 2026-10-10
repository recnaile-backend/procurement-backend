# Audit Service

RECNAILE Audit Service module. This ZIP contains the module folder intended to be placed at `services/audit-service` in the existing Gradle multi-project repository. Keep the repository root `settings.gradle`, root `build.gradle`, version catalog/conventions, contracts, and Compose file from the full repository.

## Phase status
- Phase 01: typed `AUDIT_EVENT` domain envelope and payload.
- Phase 02: MongoDB persistence document, append-only insert service, repository query methods, and documented indexes.

## Configuration
- Service port: `SERVICE_PORT` (default `8091`).
- Mongo URI base: `MONGODB_BASE_URI`; database name: `MONGODB_DATABASE` (default `audit`).
- Kafka bootstrap configuration remains present for later consumer integration.

## Commands (PowerShell, from repository root)
Delete existing module before replacing it with the ZIP contents:

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-02.zip -DestinationPath .\services -Force
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

**Compose note:** if the audit-service block is still commented out in your root Compose file, Docker Compose will not start it until that block is enabled and its environment/network settings are checked. This ZIP does not silently rewrite the root Compose file.

## Git summary — Phase 02
```powershell
git add services/audit-service
git commit -m "feat(audit-service): add MongoDB audit persistence"
```
