# Inventory Service

Authoritative stock ledger for the RECNAILE Procurement Platform.

## Responsibility

Inventory owns **stock**, not Item Master data.

- Master Data Service creates and maintains the canonical item using `itemId`.
- Inventory never creates a second item identity and never reads Master Data MongoDB directly.
- Inventory reads Master Data item metadata over the documented REST API.
- A newly-created Master Data item is visible from Inventory before the first GRN; if the item has explicit `availableStock`/`defectStock` opening values, those values are projected as the initial stock until the first Inventory movement.
- `availableStock` and `defectStock` in the Master Data item payload are treated as **opening-stock inputs only before the first Inventory movement**. This supports existing item masters without making Master Data the long-term stock owner.
- Once an Inventory movement exists for an item, Inventory-owned movements are authoritative. Later Master Data edits to `availableStock`/`defectStock` do not overwrite transactional stock. Use Inventory adjustments or business events for post-transaction corrections.
- `department` and `rack` from Master Data are used as metadata defaults when an Inventory balance does not have its own department/location dimension.
- `minimumStock` and `maximumStock` come from Master Data unless a balance has an explicit Inventory threshold override.

## Stock lifecycle

```text
Master Data Item
      |
      | itemId + metadata
      v
Inventory
      |
      +-- GOODS_RECEIVED       -> AVAILABLE / DEFECT
      +-- OUTWARD_ISSUED       -> AVAILABLE decrease
      +-- RETURN_RECORDED      -> GOOD: AVAILABLE increase
      |                          DAMAGED: DEFECT increase
      +-- Administrative Adjustment (controlled recovery only)
```

Inventory is the only service that mutates stock balances.

## REST APIs

### Stock reads

```text
GET /api/v1/inventory/stock
GET /api/v1/inventory/stock/{itemId}
GET /api/v1/inventory/stock/{itemId}/summary
GET /api/v1/inventory/stock/{itemId}/movements
GET /api/v1/inventory/reconciliation/{itemId}
```

Reconciliation requires `PROCUREMENT`, `TL`, or `MD`.

These APIs use the Master Data `itemId` as the canonical item identity.

### Thresholds

```text
PUT /api/v1/inventory/stock/{itemId}/thresholds
```

Required role: `PROCUREMENT` or `MD`.

The endpoint updates only Inventory's threshold override for the selected balance dimension. It never changes Master Data.

### Administrative correction

```text
POST /api/v1/inventory/stock/{itemId}/adjustments
```

Required role: `PROCUREMENT` or `MD`.

Required header:

```text
Idempotency-Key: <stable-key>
```

Request example:

```json
{
  "departmentName": "IT",
  "projectId": "PROJ-2001",
  "location": "RACK-A01",
  "stockType": "AVAILABLE",
  "delta": 5,
  "reason": "Physical stock count correction"
}
```

The normal Inward/Outward/Return event flow must be used for business transactions. Administrative adjustment exists only for controlled reconciliation/recovery.

The old `PUT /available-quantity` endpoint is intentionally removed because clients must not directly set authoritative stock quantities.

## Master Data integration

Configured with:

```text
MASTER_DATA_SERVICE_URL=http://master-data-service:8082
JWT_SECRET_KEY=<shared HS256 secret>
JWT_ISSUER=auth-service
```

Inventory creates a short-lived internal HS256 service token to call the authenticated Master Data GET APIs. No Master Data database access is performed.

For `GOODS_RECEIVED`, `itemId` is preferred. The current Inward compatibility contract may provide `itemName`; when `itemId` is absent Inventory resolves the name uniquely against Master Data and then stores the canonical `itemId`. Ambiguous or unknown names are rejected.

## Events consumed

- `GOODS_RECEIVED` from Inward/GRN
- `OUTWARD_ISSUED` from Outward
- `RETURN_RECORDED` from Return

State-changing events are idempotent and protected by unique MongoDB movement keys and optimistic concurrency.

## Security

Inventory uses the platform's **HS256** JWT configuration:

- issuer: `auth-service`
- signing key: `JWT_SECRET_KEY`
- roles: JWT `roles` claim
- GET APIs: `EMPLOYEE`, `PROCUREMENT`, `TL`, `MD`
- Thresholds and administrative adjustments: `PROCUREMENT`, `MD`

## Observability

- Actuator health/metrics/prometheus
- Micrometer inventory counters
- OpenTelemetry tracing/export
- Immutable stock movement history
- Transactional outbox for low-stock alerts
- Audit records for stock changes and administrative corrections

## Runtime

| Property | Value |
|---|---|
| Service | `inventory-service` |
| Port | `8086` |
| Swagger | `http://localhost:8086/swagger-ui.html` |
| OpenAPI | `http://localhost:8086/v3/api-docs` |
| Health | `http://localhost:8086/actuator/health` |


## Return allocation rack-key correction (2026-10-09)

- Outward issue allocations now store the actual balance key returned after Inventory resolves dimensions from Master Data (including rack/location).
- Legacy allocations with a stale/incomplete key are repaired only from the corresponding `OUTWARD_ISSUED` ledger movements and only when they resolve to exactly one existing balance. Ambiguous matches fail safely instead of guessing.
- Added a regression test for an outward event without an explicit location when Master Data supplies `Rack-003`.

Validation note: the source environment used to prepare this ZIP did not have Gradle or Docker installed, so the Gradle test/build has not been executed here. Run the test/build commands below in your project environment before deploying.

```powershell
.\gradlew.bat test
.\gradlew.bat clean build
```
