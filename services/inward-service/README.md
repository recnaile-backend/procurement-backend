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


## Finalized Procurement → PO → Inward/GRN Workflow

- The issued Purchase Order is the authoritative source for `itemName` and ordered quantity.
- GRN creation accepts `itemName`, `unitOfMeasure`, accepted quantity, rejected quantity and rejection reason.
- `itemId` is not used in the procurement → PO → Inward/GRN flow.
- GRN creation validates the referenced PO is `ISSUED` and that every requested `itemName` exists exactly once on that PO.
- A created GRN starts as `DRAFT`; it does not publish `GOODS_RECEIVED`.
- `POST /api/v1/goods-receipts/{grnId}/receive` confirms the receipt, calculates `PARTIALLY_RECEIVED` or `COMPLETED`, and then writes the `GOODS_RECEIVED` event to the transactional outbox.
- Accepted + rejected quantity is validated against the PO ordered quantity cumulatively across confirmed GRNs.
