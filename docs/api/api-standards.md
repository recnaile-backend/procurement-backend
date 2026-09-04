# API Standards

## 1. Base Path & Versioning

All public APIs are served under `/api/v1/...`. A breaking change is introduced under a
new major version (`/api/v2/...`) — existing `/api/v1/` consumers are never broken
in-place. Both versions may run concurrently during a documented deprecation window.

## 2. HTTP Methods

| Method | Usage |
|---|---|
| `GET` | Read a resource or collection; safe, idempotent, cacheable where appropriate |
| `POST` | Create a resource, or trigger a non-idempotent business action (e.g., approve) |
| `PUT` | Full replace of a resource (idempotent) |
| `PATCH` | Partial update of a resource |
| `DELETE` | Remove/deactivate a resource (soft-delete preferred for audited domains) |

## 3. Response Envelope

**Success:**

```json
{
  "success": true,
  "data": { },
  "error": null
}
```

**Error:**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BUSINESS_ERROR",
    "message": "Human readable message",
    "traceId": "..."
  }
}
```

- `error.code` is a stable, machine-readable enum (e.g., `VALIDATION_ERROR`,
  `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`, `CONFLICT`, `BUSINESS_ERROR`,
  `INTERNAL_ERROR`) — defined once in `libs/common-errors` and reused by every service.
- `error.traceId` always lets support/SRE jump straight to the trace in Tempo.

## 4. Pagination

Cursor-agnostic, offset-based pagination as the default (simplicity for a back-office
system), standardized query params:

```text
GET /api/v1/indents?page=0&size=20&sort=createdAt,desc
```

Paginated `data` shape:

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

## 5. Filtering & Sorting

- Filtering via explicit query parameters per resource (documented per-endpoint in the
  OpenAPI contract) — no free-form query language exposed publicly.
- Sorting via `sort=field,direction` — repeatable for multi-field sort.

## 6. Validation

- Request validation (Jakarta Bean Validation annotations, once implemented) rejects
  invalid input with HTTP 400 and a `VALIDATION_ERROR` code, with field-level detail in
  `error.message`.

## 7. HTTP Status Codes

| Code | Meaning |
|---|---|
| 200 | Success |
| 201 | Resource created |
| 202 | Accepted (async processing triggered) |
| 400 | Validation error |
| 401 | Missing/invalid authentication |
| 403 | Authenticated but not authorized for this action |
| 404 | Resource not found |
| 409 | Conflict (e.g., duplicate submission, state conflict) |
| 422 | Business rule violation |
| 429 | Rate limited |
| 500 | Internal error |
| 503 | Downstream dependency unavailable |

## 8. Correlation & Tracing

Every request must carry (generated at the gateway if absent):

- `X-Correlation-Id` — stable across a full business transaction/saga.
- `traceparent` (W3C Trace Context) — per-hop trace/span identifiers.

Both are echoed back in error responses and included in every structured log line.

## 9. Idempotency for Unsafe Operations

State-changing POST endpoints that are safe to retry (e.g., "approve indent") accept an
optional `Idempotency-Key` header; the service (via Redis or a MongoDB unique index on
the key) ensures a retried request with the same key does not double-apply the action.
