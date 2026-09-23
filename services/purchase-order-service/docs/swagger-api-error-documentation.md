# Purchase Order Service — Swagger API Error Documentation

## Standard error envelope

All documented API errors use the platform error envelope:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PO_NOT_FOUND",
    "message": "Purchase Order not found.",
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
  }
}
```

### Fields

| Field | Meaning |
|---|---|
| `success` | Always `false` for an error |
| `data` | `null` for an error response |
| `error.code` | Stable machine-readable error code |
| `error.message` | Human-readable explanation |
| `error.traceId` | Trace identifier used to correlate the request across logs/traces |

## HTTP error catalogue

| HTTP | Error code(s) / category | When it occurs |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Invalid request body or parameter |
| 400 | Request/header validation | Required request data is missing or malformed |
| 401 | `AUTHENTICATION_REQUIRED` | JWT is missing or invalid |
| 403 | `ACCESS_DENIED` | Authenticated role cannot perform the operation |
| 404 | `PO_NOT_FOUND` | Purchase Order does not exist |
| 404 | `VENDOR_NOT_FOUND` | Referenced vendor does not exist |
| 404 | Other resource-not-found codes | Referenced resource does not exist |
| 409 | Idempotency conflict | Same idempotency key is reused for a different request |
| 409 | `IDEMPOTENCY_REQUEST_IN_PROGRESS` | Identical command is already being processed |
| 409 | `PO_CONCURRENT_MODIFICATION` | Optimistic concurrency detects a stale version |
| 409 | Business-rule errors | Requested lifecycle transition or business operation is not allowed |
| 503 | `MASTER_DATA_UNAVAILABLE` | Master Data Service cannot be reached |
| 503 | `MASTER_DATA_CLIENT_ERROR` | Master Data integration returns a client/integration failure |
| 500 | `INTERNAL_ERROR` | Unexpected server-side failure |

## Idempotency-Key

Create and lifecycle command APIs accept an optional `Idempotency-Key`.

When supplied, the same key may safely be retried for the same request. A key must not be reused for a different request.

## Authentication and authorization

Swagger uses HTTP Bearer authentication with a JWT.

Supported roles:

- `Employee` — no PO API access
- `Procurement` — view/create/issue/cancel/complete
- `TL` — view only
- `MD` — full access

## Troubleshooting

Use `error.traceId` when correlating a failed request with application logs, OpenTelemetry traces, and centralized observability tooling.
