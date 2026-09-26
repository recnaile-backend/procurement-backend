# GRN REST API — Phase 08

## Canonical API base path

`/api/v1/goods-receipts`

The earlier `/api/v1/grns` aliases have been removed. There is one canonical API for each GRN operation.

All business responses use the platform envelope:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

Errors use:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ITEM_NOT_IN_PURCHASE_ORDER",
    "message": "Item Item-999 does not belong to PO PO-008",
    "traceId": "..."
  }
}
```

## APIs

### 1. GET `/api/v1/goods-receipts`

Returns all GRNs.

Optional query parameter:

- `poId` — optional Purchase Order ID. When supplied, only GRNs for that PO are returned.

Examples:

```http
GET /api/v1/goods-receipts
```

Returns all GRNs.

```http
GET /api/v1/goods-receipts?poId=PO-008
```

Returns all GRNs created against `PO-008`.

There is intentionally **no `status`, `page`, or `size` query parameter** in Phase 08.

### 2. GET `/api/v1/goods-receipts/{grnId}`

Returns one GRN by its unique identifier.

Example:

```http
GET /api/v1/goods-receipts/GRN%2F26-27%2F001

> Swagger UI input: enter `GRN/26-27/001` (with literal `/`). Swagger URL-encodes the slashes to `%2F`; the stored/returned business ID remains `GRN%2F26-27%2F001`.
```

Returns `404 GRN_NOT_FOUND` when the identifier does not exist.

### 3. POST `/api/v1/goods-receipts`

Creates a DRAFT GRN against an ISSUED PO. The PO is revalidated through Purchase Order Service before persistence.

Each submitted item must belong to the referenced PO. Rejected quantity requires a rejection reason. Cumulative received/outstanding quantity validation is implemented in Phase 06.

## Error documentation

| HTTP | Code | Applies to | Meaning |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | POST | Bean validation failed for the request body |
| 400 | `INVALID_GRN_REQUEST` | GET/POST | General malformed GRN request |
| 400 | `INVALID_PARAMETER` | GET/POST | Request parameter has an invalid type or value |
| 400 | `ITEM_NOT_IN_PURCHASE_ORDER` | POST | Requested item does not belong to the referenced PO |
| 400 | `DUPLICATE_ITEM_IN_PURCHASE_ORDER` | POST | Duplicate item ID was detected where uniqueness is required |
| 400 | `REJECTION_REASON_REQUIRED` | POST | Rejected quantity is greater than zero but no rejection reason was supplied |
| 400 | `DUPLICATE_ITEM_IN_GRN` | POST | The same item was submitted more than once in one GRN |
| 401 | — | All | Authentication is missing or invalid |
| 403 | — | All | Authenticated user does not have permission for the operation |
| 404 | `GRN_NOT_FOUND` | GET by ID | Requested GRN does not exist |
| 404 | `PURCHASE_ORDER_NOT_FOUND` | POST | Referenced PO does not exist in Purchase Order Service |
| 409 | `PURCHASE_ORDER_NOT_ISSUED` | POST | Referenced PO exists but its current status is not `ISSUED` |
| 409 | `RECEIVED_QUANTITY_EXCEEDS_ORDERED` | POST | Cumulative accepted + rejected quantity would exceed the PO ordered quantity |
| 409 | `DUPLICATE_DELIVERY_NOTE` | POST | A GRN already exists for the same PO and delivery note |
| 502 | `PURCHASE_ORDER_SERVICE_ERROR` | POST | Purchase Order Service could not be queried successfully or returned an invalid response |
| 500 | `INTERNAL_SERVER_ERROR` | All | Unexpected server-side failure |

### Error response examples

#### 400 — Item not in PO

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ITEM_NOT_IN_PURCHASE_ORDER",
    "message": "Item Item-999 does not belong to PO PO-008",
    "traceId": "corr-123"
  }
}
```

#### 404 — GRN not found

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "GRN_NOT_FOUND",
    "message": "GRN not found: 6ab4adf6d80b3f36a37afb99",
    "traceId": "corr-123"
  }
}
```

#### 404 — Purchase Order not found

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PURCHASE_ORDER_NOT_FOUND",
    "message": "Purchase Order P0-009 was not found",
    "traceId": "corr-123"
  }
}
```

Use the exact PO ID returned by the Purchase Order Service. For example, `P0-009` (zero) and `PO-009` (letter O) are different identifiers.

#### 409 — PO not issued

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PURCHASE_ORDER_NOT_ISSUED",
    "message": "Purchase Order PO-008 cannot be used for goods receipt because its current status is DRAFT. The Purchase Order must be ISSUED before a GRN can be created.",
    "traceId": "corr-123"
  }
}
```

#### 502 — PO Service error

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PURCHASE_ORDER_SERVICE_ERROR",
    "message": "Purchase Order Service returned an unsuccessful response",
    "traceId": "corr-123"
  }
}
```

## Phase 06/07 — Quantity and Receipt Progress Rules

For every PO item, `acceptedQuantity + rejectedQuantity` is the quantity consumed
from the ordered quantity. The service validates both the current GRN and the
cumulative quantity across existing GRNs for the same PO. A receipt is rejected
with `RECEIVED_QUANTITY_EXCEEDS_ORDERED` when the cumulative quantity would exceed
the ordered quantity.

Multiple GRNs are allowed for the same PO to support partial deliveries. The same
`poId + deliveryNoteNumber` combination is not allowed. The service also calculates
remaining quantity per PO item and whether every PO line has been fully received.
The Purchase Order Service remains the owner of the PO lifecycle status; the GRN
`grnStatus` remains the GRN lifecycle status and is not changed merely because a
PO becomes fully received.


## GRN Business Number

Each created Goods Receipt Note receives a backend-generated business number such as `GRN%2F26-27%2F001`, `GRN%2F26-27%2F002`, `GRN%2F26-27%2F003`, and so on. The sequence is maintained atomically in MongoDB so concurrent GRN creation requests receive distinct numbers. The technical MongoDB `id` remains separate from the business `grnId`. The business identifier itself is stored in encoded form, matching the Purchase Order Service convention. The financial year follows the April-March Indian financial year and the sequence restarts at `001` each financial year. For example, the stored and returned identifier is `GRN%2F26-27%2F001`.

## Security & RBAC

All `/api/v1/goods-receipts/**` business APIs require a valid JWT.

- GET goods-receipt APIs: `EMPLOYEE`, `PROCUREMENT`, `TL`, `MD`, `ADMIN`
- POST goods-receipt API: `PROCUREMENT`, `ADMIN`
- Missing/invalid JWT: HTTP 401 with `UNAUTHORIZED`
- Authenticated user without required role: HTTP 403 with `FORBIDDEN`
- Swagger/OpenAPI and health/info/prometheus endpoints remain public.
- Roles are read from the JWT `roles` claim and converted to Spring `ROLE_*` authorities.
