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

This service does not modify inventory, create an issue transaction, publish `OUTWARD_ISSUED`, or create assets in this phase.

## Verification

From the repository root:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:test
```

Then:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:build
```


## Approval workflow

The service now supports the authenticated approval lifecycle:

- `POST /api/v1/outward/{outwardNumber}/submit` — requester submits a `DRAFT` request.
- `POST /api/v1/outward/{outwardNumber}/approve` — configured approval role changes `PENDING_APPROVAL` to `APPROVED`.
- `POST /api/v1/outward/{outwardNumber}/reject` — configured approval role changes `PENDING_APPROVAL` to `REJECTED`.

Approval roles are configured with `OUTWARD_APPROVAL_ROLES` and default to `TL,MD`. JWT roles are read from the `roles` claim and mapped to Spring `ROLE_*` authorities.

The service never modifies inventory stock during approval. Inventory mutation and the `OUTWARD_ISSUED` event remain later workflow responsibilities.


## Issue transaction

The service now supports issuing an approved outward request:

- `POST /api/v1/outward/{outwardNumber}/issue` — creates the issue transaction and changes the request from `APPROVED` to `ISSUED`.
- The issuer is taken from the authenticated JWT identity.
- The issue transaction captures the approved request snapshot, recipient, approver, issuer, items, quantities, project, department and purpose.
- A unique `outwardNumber` prevents more than one issue transaction for the same outward request.
- The operation uses MongoDB transaction management so creation of the issue transaction and the request state transition are committed together when MongoDB transaction support is available (for example MongoDB Atlas replica sets).
- This phase does not modify inventory MongoDB and does not publish `OUTWARD_ISSUED`; those are later integration responsibilities.

Issue errors include `OUTWARD_NOT_APPROVED`, `OUTWARD_ALREADY_ISSUED`, `OUTWARD_CONCURRENT_MODIFICATION`, and `OUTWARD_NUMBER_REQUIRED`.
