# Inward Service

Goods Receipt Note (GRN) service for the Recnaile procurement platform.

## Phase 13 — Audit Integration

- GRN creation writes a `GRN_CREATED` audit event to the transactional outbox.
- Audit events are published asynchronously to Kafka topic `procurement.events` by the existing outbox publisher.
- Audit events preserve authenticated actor, roles, entity/action, status change context, correlation ID and trace ID.
- GRN and audit outbox records are written in the same MongoDB transaction.
- `GOODS_RECEIVED` remains the inventory-facing business event.
- `poLineId` is not part of the GRN request, GRN line, `GOODS_RECEIVED` event, or audit contract.

## Build

```powershell
.\\gradlew :services:inward-service:clean :services:inward-service:build
```

## Docker

```powershell
docker compose build inward-service
docker compose up -d --force-recreate inward-service
docker compose ps inward-service
```

## Phase 14 — Observability

The service exposes Actuator/Prometheus metrics, OpenTelemetry traces, correlation-aware JSON logs, and liveness/readiness health endpoints. See `docs/observability.md` for the complete operational contract.
