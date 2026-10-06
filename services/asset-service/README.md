# Asset Service — Phase 2: Asset Domain Model

RECNAILE Asset Service domain foundation for reusable / asset-tracked items.

## Phase 2 scope

- Asset aggregate mapped to MongoDB collection `assets`
- Stable Asset ID value object
- Required Asset ID format: `ASSET%2FYY-YY%2FNNN`
- Asset ID is a server-generated value; clients must not supply it during creation
- Asset item and approved/issued issue reference
- Asset name, brand and model
- `departmentName` and assigned employee snapshot fields
- Issued-by actor reference
- Asset lifecycle status: `IN_USE`, `RETURNED`, `UNDER_REPAIR`, `RETIRED`
- Asset condition: `GOOD`, `DAMAGED`, `UNDER_REPAIR`
- Remarks and UTC timestamps
- Optimistic concurrency with MongoDB `@Version`
- MongoDB indexes for item, issue reference, department name, assigned user and status
- Domain validation tests

## Asset ID convention

The persisted Asset ID is intentionally the encoded value itself:

```text
ASSET%2F26-27%2F001
ASSET%2F26-27%2F002
ASSET%2F26-27%2F003
```

`%2F` is part of the actual Asset ID value. It is not converted back to `/` in the database.

The financial-year sequence generator is implemented in the later creation/generation phase. This phase only validates the domain identifier format.

## Important boundaries

The Asset aggregate is not a stock ledger. Quantity, stock movements and inventory balances belong to Inventory Service.

Assignment history is deliberately not implemented in this phase; it is handled by the assignment-history phase so historical records can be retained independently.

State-transition enforcement is also implemented in the lifecycle/state-machine phase.

## Build and test from repository root

```powershell
Remove-Item -Recurse -Force .\services\asset-service
```

After extracting this phase into `services/asset-service`:

```powershell
.\gradlew :services:asset-service:clean :services:asset-service:test :services:asset-service:build
```

## Docker

The Dockerfile follows the repository's existing multi-stage microservice pattern and builds the service inside Docker, so the root `.dockerignore` can continue to exclude `build/`.

```powershell
docker compose build --no-cache asset-service
docker compose up -d asset-service
docker compose ps asset-service
docker compose logs --tail=100 asset-service
```

Health check:

```powershell
curl.exe http://localhost:8089/actuator/health
```

Expected:

```json
{"status":"UP"}
```

## Later phases

REST APIs, assignment history, lifecycle state machine, Kafka event consumers, idempotency, audit integration, security/RBAC, observability expansion and full integration tests are implemented in their respective phases.
