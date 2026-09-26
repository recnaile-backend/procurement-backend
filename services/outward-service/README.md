# Outward Service — Master Data Validation

This version adds synchronous Master Data validation to the outward request workflow.

## Validation performed

Before a request is persisted:

- Every requested item must exist in Master Data.
- Every requested item must be `ACTIVE`.
- Requested quantity must not exceed the current `availableStock` returned by Master Data.
- The supplied `departmentName` must match an `ACTIVE` Master Data department.
- An optional `projectId` must identify an `ACTIVE` Master Data project.
- Duplicate item IDs in one request are rejected.
- Asset tracking is derived from Master Data; the client cannot declare it.

The service communicates with Master Data through its documented REST APIs and never reads the Master Data MongoDB directly.

## Downstream configuration

```text
MASTER_DATA_BASE_URL=http://master-data-service:8082
MASTER_DATA_CONNECT_TIMEOUT_MS=2000
MASTER_DATA_READ_TIMEOUT_MS=3000
```

The authenticated bearer token, correlation ID and W3C `traceparent` are propagated to the synchronous Master Data request.

## Error codes

Examples include:

- `OUTWARD_ITEM_NOT_FOUND`
- `OUTWARD_ITEM_INACTIVE`
- `OUTWARD_INSUFFICIENT_STOCK`
- `OUTWARD_DEPARTMENT_NOT_FOUND`
- `OUTWARD_PROJECT_NOT_FOUND`
- `OUTWARD_PROJECT_INACTIVE`
- `MASTER_DATA_SERVICE_UNAVAILABLE`

A Master Data availability failure returns HTTP 503. A business validation failure returns HTTP 400.

## Boundary

This validation phase does not modify inventory, create an issue transaction, publish `OUTWARD_ISSUED`, create assets, or implement approval/RBAC.

## Verification

From the repository root:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:test
```

Then:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:build
```
