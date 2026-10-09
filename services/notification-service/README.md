# notification-service

Event-driven orchestration service for in-app and email notifications.

- **Port:** `8090`
- **MongoDB database:** `notification`
- **Gateway base path:** `/api/v1/notification` (routes must be verified against the active gateway configuration)
- **Java:** 21
- **Kafka consumer group:** `notification-service` (configurable)

## Current implementation stage

This service is being implemented in phases. The initial generic CRUD endpoint has been removed because it exposed unrestricted create/update/delete operations over an untyped record and was not a notification API. The domain foundation now models a notification per recipient and channel, separates delivery state from read state, and carries event/correlation/trace identifiers. Kafka consumer properties are configured for manual acknowledgment; actual listeners, persistence repositories, deduplication, recipient resolution, templates, delivery providers, APIs and retry/DLT replay are subsequent phases.

## Event inputs

The platform's Kafka topic catalogue groups domain events under `procurement.indent.events`, `procurement.po.events`, `procurement.inward.events`, `procurement.inventory.events`, `procurement.outward.events`, `procurement.return.events`, and `procurement.asset.events`. Event contracts in `contracts/events/` remain authoritative for payload shape. `GOODS_RECEIVED.schema.json` currently defines its own envelope and uses a non-UUID `eventId` constraint, unlike the shared `_envelope.schema.json`; align this contract in a separately reviewed contract change rather than silently rewriting it here.

## Configuration

Set `MONGODB_BASE_URI` and Kafka/OTLP environment variables in the deployment environment. Do not commit credentials or include them in logs.

## Roadmap status

- Phase 01 — project and contract audit: reviewed the provided service, event schemas and Kafka topic catalogue; see findings above.
- Phase 02 — service foundation and domain model: initial model/configuration added; build verification is still required in the full Gradle repository.
- Phases 03–10: not yet implemented.


## Phase 02 validation and delivery

This phase provides the notification domain foundation only. Kafka listeners, MongoDB repositories, idempotency enforcement, recipient resolution, templates, delivery providers, public APIs and retry/DLT operations are intentionally not claimed as implemented.

### Run from the repository root (PowerShell)

Back up the current service before replacing it. Extract this archive, then copy its `notification-service` directory to `services/notification-service`.

```powershell
.\gradlew.bat :services:notification-service:test
.\gradlew.bat :services:notification-service:build
docker compose build notification-service
docker compose up -d notification-service
docker compose ps notification-service
```

The commands must be run in the full repository with the root Gradle wrapper and the configured Compose service. Test/build results must be confirmed in that environment; this ZIP is not represented as having passed the full repository build.

### Git commit

```powershell
git add services/notification-service
git commit -m "feat(notification): establish notification domain foundation"
```


## Phase 02 validation and delivery

This phase provides the notification domain foundation only. Kafka listeners, MongoDB repositories, idempotency enforcement, recipient resolution, templates, delivery providers, public APIs and retry/DLT operations are intentionally not claimed as implemented.

### Run from the repository root (PowerShell)

Back up the current service before replacing it. Extract this archive, then copy its `notification-service` directory to `services/notification-service`.

```powershell
.\gradlew.bat :services:notification-service:test
.\gradlew.bat :services:notification-service:build
docker compose build notification-service
docker compose up -d notification-service
docker compose ps notification-service
```

The commands must be run in the full repository with the root Gradle wrapper and the configured Compose service. Test/build results must be confirmed in that environment; this ZIP is not represented as having passed the full repository build.

### Git commit

```powershell
git add services/notification-service
git commit -m "feat(notification): establish notification domain foundation"
```
