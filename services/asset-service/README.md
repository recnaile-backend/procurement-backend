# RECNAILE Asset Service - Phase 3

Phase 3 adds assignment history for reusable/asset-tracked assets. The service keeps a current assignment snapshot on the `Asset` aggregate and stores every assignment lifecycle in the `asset_assignments` MongoDB collection. Historical records are closed, never deleted.

## Runtime
- Java 21
- Spring Boot 3.5.16
- Gradle
- MongoDB database: `assetdb`
- Service port: `8089`
- Asset ID format: `ASSET%2F26-27%2F001`

## Phase 3 scope
- `AssetAssignment` historical record
- Active-assignment uniqueness per asset
- Assignment and close-assignment application service
- Employee and department assignment-history queries
- Current assignment snapshot on `Asset`
- MongoDB indexes and optimistic versioning
- Unit tests for assignment lifecycle/history

## Assignment history rules
1. An asset may have at most one active assignment.
2. Creating an assignment updates the asset's current assigned user and department.
3. Closing an assignment records `returnedAt` and marks the historical record inactive.
4. Closing clears the current assignment snapshot but does not delete the historical record.
5. Historical assignment records are never deleted by this service.
6. Lifecycle status transitions are intentionally handled in a later phase.

## Docker
The Dockerfile follows the repository's existing multi-stage service pattern: Gradle builds `bootJar` inside the build stage and the runtime stage runs the generated JAR as UID 10001.

## Verification from repository root
```powershell
Remove-Item -Recurse -Force .\services\asset-service
```
Extract this service into `services/asset-service`, then:

```powershell
.\gradlew :services:asset-service:clean :services:asset-service:test :services:asset-service:build
docker compose build --no-cache asset-service
docker compose up -d asset-service
docker compose ps asset-service
curl.exe http://localhost:8089/actuator/health
```

Expected health response:
```json
{"status":"UP"}
```


## Phase 4 — Lifecycle State Machine

Lifecycle transitions are enforced by the Asset aggregate:

- `RETURNED -> IN_USE` on a valid assignment.
- `IN_USE -> RETURNED` for a good return.
- `IN_USE -> UNDER_REPAIR` for a damaged/repair return.
- `UNDER_REPAIR -> RETURNED` after repair completion.
- `RETURNED -> RETIRED` for retirement.
- `RETIRED` is terminal.

Assignment history is closed on return and is never deleted.
