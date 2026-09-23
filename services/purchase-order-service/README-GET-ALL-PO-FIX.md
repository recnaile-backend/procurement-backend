# Purchase Order Service - GET All Purchase Orders

## Endpoint

`GET /api/v1/purchase-orders`

### Supported optional filters

Only these query parameters are supported:

- `vendorName`
- `projectId`
- `departmentName`

`status` is **not** a GET-all query parameter. The `status` field remains in each PO response.

### Filter behavior

All supplied filters are combined using **AND** semantics.

Examples:

```text
GET /api/v1/purchase-orders
GET /api/v1/purchase-orders?vendorName=3D
GET /api/v1/purchase-orders?projectId=P-1
GET /api/v1/purchase-orders?departmentName=Software
GET /api/v1/purchase-orders?vendorName=3D&projectId=P-1&departmentName=Software
```

### Legacy CLOSED compatibility

`CLOSED` is no longer part of the current PO lifecycle and there is no close API.

Older deployments may already have MongoDB documents with `status: "CLOSED"`. The current repository read queries explicitly exclude those legacy documents. This prevents Spring Data MongoDB from trying to deserialize the removed `CLOSED` enum value and turning a normal GET-all request into HTTP 500.

The legacy documents should still be removed/migrated during database cleanup; the read-side guard is intentionally kept so the current API remains stable while old data exists.
