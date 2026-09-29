# Outward Service — API Response & Error Documentation

**API base path:** `/api/v1/outward`  
**Authentication:** JWT Bearer token  
**Content-Type:** `application/json`  
**Timestamps:** ISO-8601 UTC (`Instant`)  

This document is the response/error contract for the currently implemented Outward Service endpoints. It documents the actual DTOs and error codes implemented by the service; it does not introduce undocumented business behavior.

## 1. Response conventions

### 1.1 Successful responses

Successful business endpoints return their DTO directly as JSON. They are **not** wrapped in a `success/data` envelope.

| HTTP | Endpoint | Response body |
|---|---|---|
| `201 Created` | `POST /api/v1/outward` | `OutwardResponse` |
| `200 OK` | `POST /api/v1/outward/{outwardNumber}/submit` | `OutwardResponse` |
| `200 OK` | `POST /api/v1/outward/{outwardNumber}/approve` | `OutwardResponse` |
| `200 OK` | `POST /api/v1/outward/{outwardNumber}/reject` | `OutwardResponse` |
| `200 OK` | `POST /api/v1/outward/{outwardNumber}/issue` | `OutwardIssueTransactionResponse` |

### 1.2 Standard application errors

Business, validation, Master Data and downstream failures are returned as the `ApiError` object:

```json
{
  "code": "OUTWARD_NOT_FOUND",
  "message": "Outward request not found",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "timestamp": "2026-09-29T10:30:00Z"
}
```

Fields:

| Field | Type | Meaning |
|---|---|---|
| `code` | string | Stable machine-readable error code. Clients should branch on this value rather than parsing `message`. |
| `message` | string | Human-readable failure description. |
| `traceId` | string/null | Trace identifier when available; use it when investigating a failed request. |
| `timestamp` | ISO-8601 string | UTC time at which the error response was generated. |

### 1.3 Security-filter errors

Requests rejected by Spring Security before controller execution use the service's security error shape:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AUTHENTICATION_REQUIRED",
    "message": "A valid Bearer access token is required",
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
  }
}
```

Typical codes are `AUTHENTICATION_REQUIRED` (`401`) and `ACCESS_DENIED` (`403`). These responses are produced by the security filter chain rather than `OutwardExceptionHandler`.

## 2. `OutwardResponse`

Returned after create, submit, approve and reject operations.

| Field | Type | Description |
|---|---|---|
| `id` | string | MongoDB document identifier. |
| `outwardNumber` | string | Server-generated outward number, stored URL-encoded, e.g. `OUT%2F26-27%2F001`. |
| `requestedAt` | ISO-8601 string | Request creation time. |
| `items` | array | Requested item rows. |
| `items[].itemId` | string | Master Data item identifier. |
| `items[].quantity` | number | Requested quantity. |
| `projectId` | string/null | Optional Master Data project. |
| `requestedBy` | string | Authenticated requester identity. |
| `departmentName` | string | Department associated with the request. |
| `purpose` | string | Business purpose. |
| `recipient` | object | Recipient employee details. |
| `recipient.employeeId` | string | Recipient employee identifier. |
| `recipient.employeeName` | string | Recipient display name. |
| `status` | enum | `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `ISSUED`, or `CANCELLED`. |
| `approvedBy` | string/null | Approver identity after approval. |
| `approvedAt` | ISO-8601/null | Approval timestamp. |
| `rejectedBy` | string/null | Rejector identity after rejection. |
| `rejectedAt` | ISO-8601/null | Rejection timestamp. |
| `rejectionReason` | string/null | Recorded rejection reason. |
| `version` | number | Optimistic-lock version. |
| `createdAt` | ISO-8601 string | Persistence creation time. |
| `updatedAt` | ISO-8601 string | Last update time. |

Example:

```json
{
  "id": "66f1...",
  "outwardNumber": "OUT%2F26-27%2F001",
  "requestedAt": "2026-09-29T10:20:00Z",
  "items": [{"itemId": "ITEM-001", "quantity": 2}],
  "projectId": "PROJ-001",
  "requestedBy": "EMP-1001",
  "departmentName": "Engineering",
  "purpose": "Laptop issue for project work",
  "recipient": {"employeeId": "EMP-1002", "employeeName": "John Doe"},
  "status": "APPROVED",
  "approvedBy": "TL-100",
  "approvedAt": "2026-09-29T10:25:00Z",
  "rejectedBy": null,
  "rejectedAt": null,
  "rejectionReason": null,
  "version": 2,
  "createdAt": "2026-09-29T10:20:00Z",
  "updatedAt": "2026-09-29T10:25:00Z"
}
```

## 3. `OutwardIssueTransactionResponse`

Returned by the issue endpoint. It represents the persisted issue transaction snapshot.

| Field | Type | Description |
|---|---|---|
| `id` | string | Issue transaction identifier. |
| `outwardNumber` | string | Source outward number. |
| `idempotencyKey` | string/null | Optional key used for replay protection. |
| `issuedAt` | ISO-8601 string | Issue timestamp. |
| `items` | array | Items and quantities issued. |
| `projectId` | string/null | Project captured at issue time. |
| `requestedBy` | string | Original requester. |
| `departmentName` | string | Department captured at issue time. |
| `recipientEmployeeId` | string | Recipient employee ID. |
| `recipientEmployeeName` | string | Recipient name. |
| `issuerId` | string | Authenticated user who issued the request. |
| `approverId` | string | Recorded approver. |
| `purpose` | string | Original purpose. |
| `assetTracked` | boolean | Whether Master Data identified the item as asset-tracked. |
| `version` | number | Optimistic-lock version of the issue transaction. |
| `createdAt` | ISO-8601 string | Persistence creation time. |

## 4. Endpoint response documentation

### Create outward
`POST /api/v1/outward`

**Success:** `201 Created` → `OutwardResponse` with initial `status = DRAFT`.

**Documented errors:**
- `400` → validation/Master Data business error.
- `401` → authentication required.
- `503` → Master Data service unavailable.

### Submit
`POST /api/v1/outward/{outwardNumber}/submit`

**Success:** `200 OK` → `OutwardResponse`, normally with `status = PENDING_APPROVAL`.

**Documented errors:**
- `401` → authentication required.
- `403` → requester ownership/authorization failure.
- `404` → outward does not exist.
- `409` → invalid state transition or optimistic-lock conflict.

### Approve
`POST /api/v1/outward/{outwardNumber}/approve`

**Success:** `200 OK` → `OutwardResponse` with `status = APPROVED`.

**Documented errors:**
- `401` → authentication required.
- `403` → authenticated role is not configured for outward approval.
- `404` → outward does not exist.
- `409` → invalid state transition or concurrent modification.

### Reject
`POST /api/v1/outward/{outwardNumber}/reject`

**Success:** `200 OK` → `OutwardResponse` with `status = REJECTED`.

**Request body is optional:** `{ "reason": "..." }`. Maximum reason length is 500 characters.

**Documented errors:**
- `400` → invalid rejection request/validation.
- `401` → authentication required.
- `403` → authenticated role is not configured for outward approval.
- `404` → outward does not exist.
- `409` → invalid state transition or concurrent modification.

### Issue
`POST /api/v1/outward/{outwardNumber}/issue`

**Success:** `200 OK` → `OutwardIssueTransactionResponse`.

`Idempotency-Key` is optional. When supplied, repeating the same key for the same outward request returns the existing issue transaction; reusing the key for another outward is rejected.

**Documented errors:**
- `400` → invalid outward number, invalid idempotency key, invalid item/quantity, or invalid request.
- `401` → authentication required.
- `404` → outward does not exist.
- `409` → not approved, already issued, concurrent modification, or idempotency key reused.

## 5. Error-code catalogue

| HTTP | Code | Meaning / client action |
|---:|---|---|
| 400 | `OUTWARD_VALIDATION_ERROR` | Request DTO validation failed. Fix the reported fields. |
| 400 | `OUTWARD_INVALID_REQUEST` | General invalid request/argument. |
| 400 | `OUTWARD_NUMBER_REQUIRED` | Required outward number was missing. |
| 400 | `OUTWARD_ITEMS_REQUIRED` | Issue operation has no items. |
| 400 | `OUTWARD_INVALID_ITEM` | Issue snapshot contains an invalid item or quantity. |
| 400 | `OUTWARD_IDEMPOTENCY_KEY_INVALID` | Idempotency key exceeds 128 characters. |
| 400 | `OUTWARD_DUPLICATE_ITEM` | Same item appears more than once in the create request. |
| 400 | `OUTWARD_ITEM_NOT_FOUND` | Requested item does not exist in Master Data. |
| 400 | `OUTWARD_ITEM_INACTIVE` | Requested item is not active. |
| 400 | `OUTWARD_ITEM_STOCK_UNAVAILABLE` | Master Data did not provide available stock. |
| 400 | `OUTWARD_INSUFFICIENT_STOCK` | Requested quantity exceeds available stock. |
| 400 | `OUTWARD_DEPARTMENT_NOT_FOUND` | Active department was not found. |
| 400 | `OUTWARD_PROJECT_NOT_FOUND` | Supplied project was not found. |
| 400 | `OUTWARD_PROJECT_INACTIVE` | Supplied project is not active. |
| 400 | `OUTWARD_INVALID_STATE_TRANSITION` | Requested workflow transition is not allowed from the current state. |
| 401 | `AUTHENTICATION_REQUIRED` | A valid authenticated identity is required. |
| 403 | `OUTWARD_REQUEST_OWNER_REQUIRED` | Only the requester may submit the outward. |
| 403 | `OUTWARD_APPROVAL_ROLE_NOT_ALLOWED` | Caller does not have a configured approval role. |
| 403 | `ACCESS_DENIED` | Security filter denied the operation. |
| 404 | `OUTWARD_NOT_FOUND` | Outward request does not exist. |
| 409 | `OUTWARD_NOT_APPROVED` | Only an approved outward can be issued. |
| 409 | `OUTWARD_ALREADY_ISSUED` | An issue transaction already exists. |
| 409 | `OUTWARD_CONCURRENT_MODIFICATION` | Optimistic-lock conflict; retry after refreshing state. |
| 409 | `OUTWARD_IDEMPOTENCY_KEY_REUSED` | Key belongs to another outward request. Use a new key. |
| 503 | `MASTER_DATA_SERVICE_UNAVAILABLE` | Master Data could not be reached; retry according to client policy. |

## 6. Client handling rules

1. Use the **HTTP status** for broad handling and the stable **`code`** for precise branching.
2. Do not parse the human-readable `message` to determine behavior.
3. Treat `409 OUTWARD_CONCURRENT_MODIFICATION` as retryable after refreshing the outward state.
4. Treat `503 MASTER_DATA_SERVICE_UNAVAILABLE` as a downstream availability failure; retry with bounded backoff.
5. For `OUTWARD_IDEMPOTENCY_KEY_REUSED`, generate a new key rather than retrying with the conflicting key.
6. Propagate/capture `traceId` for support and distributed-tracing investigation.
7. Do not expose JWTs, secrets or sensitive authentication data in client error messages.

## 7. Swagger/OpenAPI

Swagger UI: `/swagger-ui/index.html`  
OpenAPI JSON: `/v3/api-docs`

The controller annotations reference `ApiError` for documented error responses so the generated OpenAPI contract exposes the error schema instead of an unspecified response body.
