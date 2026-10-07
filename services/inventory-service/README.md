# Inventory Service

Authoritative inventory stock ledger and balance projection for the RECNAILE Procurement Platform.

## Responsibilities

- Consume `GOODS_RECEIVED`, `OUTWARD_ISSUED`, and `RETURN_RECORDED`.
- Maintain immutable stock movements and derived/current balances.
- Track `AVAILABLE` and `DEFECT` quantities.
- Reject operations that would make available stock negative.
- Process each event exactly once using the envelope `eventId`.
- Protect concurrent balance updates using MongoDB optimistic locking.
- Evaluate min/max thresholds and emit `LOW_STOCK_ALERT` on transition into low-stock state.
- Persist an outbox record for reliable alert publication.
- Provide reconciliation support by comparing balances with the movement ledger.
- Expose authenticated stock, threshold, movement-history, summary, and reconciliation APIs.

## Runtime

| Property | Value |
|---|---|
| Service | `inventory-service` |
| Port | `8086` |
| Base API path | `/api/v1/inventory` |
| OpenAPI JSON | `/v3/api-docs` |
| Swagger UI | `/swagger-ui.html` |
| Swagger UI alternate path | `/swagger-ui/index.html` |
| Prometheus | `/actuator/prometheus` |
| Health | `/actuator/health` |

## Swagger UI

Open the service directly:

```text
http://localhost:8086/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8086/v3/api-docs
```

The Swagger UI includes a JWT **Authorize** button. Click **Authorize** and enter:

```text
Bearer <your-access-token>
```

Swagger UI then sends the token in the `Authorization` header for the documented Inventory APIs.

The OpenAPI definition is configured in:

```text
src/main/java/com/recnaile/inventory/config/OpenApiConfig.java
```

The JWT security scheme is HTTP Bearer with `bearerFormat=JWT`.

## REST API Documentation

### 1. Get All Stock Balances

```http
GET /api/v1/inventory/stock
```

Returns all current stock balance projections across all inventory items. No query parameters are required. This endpoint is intended for inventory dashboards, admin views, and stock-overview screens.

**Required role:** `EMPLOYEE`, `PROCUREMENT`, `TL`, or `MD`

Example:

```text
GET /api/v1/inventory/stock
```

---

### 2. Get Stock Balances

```http
GET /api/v1/inventory/stock/{itemId}
```

Returns all balance projections for an item across the available department-name/project/location dimensions.

**Required role:** `EMPLOYEE`, `PROCUREMENT`, `TL`, or `MD`

Example:

```text
GET /api/v1/inventory/stock/ITEM-1001
```

---

### 3. Get Inventory Summary

```http
GET /api/v1/inventory/stock/{itemId}/summary
```

Returns the aggregated inventory view:

- available quantity
- defect quantity
- total quantity
- minimum stock
- maximum stock
- low-stock state
- current stock status

**Required role:** `EMPLOYEE`, `PROCUREMENT`, `TL`, or `MD`

Example:

```text
GET /api/v1/inventory/stock/ITEM-1001/summary
```

---

### 4. Get Stock Movement History

```http
GET /api/v1/inventory/stock/{itemId}/movements
```

Returns all immutable stock movements for the item, newest movements first. No query parameters are required.

Example:

```text
GET /api/v1/inventory/stock/ITEM-1001/movements
```

**Required role:** `EMPLOYEE`, `PROCUREMENT`, `TL`, or `MD`

---

### 5. Update Stock Thresholds

```http
PUT /api/v1/inventory/stock/{itemId}/thresholds
```

Updates the minimum and maximum stock thresholds for an item balance.

**Required role:** `PROCUREMENT` or `MD`

Request:

```json
{
  "departmentName": "IT",
  "projectId": "PROJ-2001",
  "location": "RACK-A01",
  "minimumStock": 10,
  "maximumStock": 50
}
```

Validation:

- `minimumStock >= 0`
- `maximumStock >= 0`
- `maximumStock >= minimumStock`
- Available quantity and status are recalculated by the service.

---

### 6. Reconcile Inventory

```http
GET /api/v1/inventory/reconciliation/{itemId}
```

Compares projected stock balances against the immutable movement ledger for the item.

**Required role:** `EMPLOYEE`, `PROCUREMENT`, `TL`, or `MD`

Example:

```text
GET /api/v1/inventory/reconciliation/ITEM-1001
```

## Authentication and Authorization

Inventory APIs use the same JWT resource-server model as the Procurement Platform.

- Algorithm: HS256
- Header: `Authorization: Bearer <JWT>`
- Roles are read from the JWT `roles` claim.
- Stateless authentication.
- GET inventory APIs: `EMPLOYEE`, `PROCUREMENT`, `TL`, `MD`
- Threshold update: `PROCUREMENT`, `MD`

Swagger/OpenAPI and health endpoints are public so that documentation and health probes can be accessed without a business JWT.

## Standard Error Envelope

Business/security errors follow this general structure:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid min/max stock thresholds",
    "traceId": "..."
  }
}
```

Common codes include:

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | Invalid request/threshold values |
| 401 | `AUTHENTICATION_REQUIRED` | Missing or invalid JWT |
| 403 | `ACCESS_DENIED` | Authenticated user lacks the required role |
| 409 | `BUSINESS_ERROR` | Business conflict such as insufficient available stock |

## Event Contracts

The service consumes:

- `GOODS_RECEIVED`
- `OUTWARD_ISSUED`
- `RETURN_RECORDED`

It publishes:

- `LOW_STOCK_ALERT`

Low-stock alerts are persisted through the transactional outbox before publication.

## Important Contract Limitation

The current inbound event schemas do not provide a location on `GOODS_RECEIVED` or `RETURN_RECORDED`. The Inventory API exposes the department as `departmentName`; the event consumer accepts `departmentName` and temporarily falls back to an upstream `departmentId` for compatibility until the producer contracts are aligned.

Therefore, the service supports department-name/project/location dimensions in its balance model, but it cannot infer a distinct physical rack/location from those events unless the producing contract supplies that dimension. Missing dimensions are kept as the default/null dimension rather than invented.

## Verification Checklist

After starting the service through Docker Compose:

```powershell
docker compose up -d inventory-service
docker compose ps inventory-service
curl.exe http://localhost:8086/actuator/health
```

Then open:

```text
http://localhost:8086/swagger-ui.html
```

Verify each documented operation with a valid JWT:

1. Get stock balances
2. Get inventory summary
3. Get movement history
4. Update thresholds using `PROCUREMENT` or `MD`
5. Reconcile inventory
6. Verify movement history is returned newest first without pagination parameters

Also verify:

- `401` without a JWT
- `403` with an insufficient role
- `400` for invalid threshold values
- `409` for insufficient-stock business conflicts where applicable

## Configuration

SpringDoc configuration is in `src/main/resources/application.yml`.

The service uses:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
    try-it-out-enabled: true
    display-request-duration: true
    deep-linking: true
```

No credentials or secrets are stored in this project. Runtime secrets such as `JWT_SECRET_KEY` and MongoDB connection information must be supplied through the environment/.env configuration.

## Build

The service uses Spring Boot, Java 21, Gradle, SpringDoc OpenAPI, MongoDB, Kafka, Micrometer, and OpenTelemetry.

From the procurement-backend root:

```powershell
.\gradlew :services:inventory-service:clean :services:inventory-service:test
.\gradlew :services:inventory-service:build
```

## API Data Type and Naming Conventions

- Inventory API resource identifiers are exposed as `id` instead of the generic `id`.
- Department is exposed as `departmentName` instead of `departmentId`.
- Stock quantities, thresholds, movement quantity and movement delta are integer values.
- Stock values are system-derived; clients do not directly set available or defect stock.

### Movement `delta`

`delta` is the signed change applied to the stock balance by that movement. For example, a goods receipt of 50 has `quantity = 50` and `delta = +50`; an outward issue of 10 has `quantity = 10` and `delta = -10`. The movement `quantity` describes the movement size, while `delta` describes its effect on the balance.

### Reconciliation result

The reconciliation API compares the current projected balance with the sum of immutable movement deltas. It returns the balance value, ledger-derived value, their difference, match flags and an overall `reconciled` flag. This is useful for detecting stock projection/ledger inconsistencies without modifying historical movements.


### Update available stock

`PUT /api/v1/inventory/stock/{itemId}/available-quantity` is restricted to Procurement and MD and records the difference as an immutable manual adjustment movement.

## Master Data Item Identity

`itemId` is the canonical inventory identity and is owned by the Master Data Service.
Procurement creates/maintains item masters there; Inventory does not create independent item identities.
All Inventory REST APIs use the Master Data `itemId` in their path.

For event-driven stock changes, the producer must provide the canonical `itemId` in the event payload. Inventory must never invent an item ID from `itemName`. If a producer currently has only `itemName`, that producer must resolve the item through Master Data before publishing the Inventory-consumed event.

## Audit and Alert Guarantees

- Available-stock adjustments create an immutable movement and an audit record.
- Threshold changes create an audit record with before/after threshold and status values.
- A transition into low-stock state creates a `LOW_STOCK_ALERT` outbox record whether the transition was caused by a stock movement, manual adjustment, or threshold change.
- Processed event IDs are protected by a unique MongoDB index to prevent concurrent duplicate event processing.
