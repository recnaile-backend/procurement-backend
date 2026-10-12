## RBAC policy

`MD` is the highest-privilege role in this application. All `/api/v1/reporting/admin/**` endpoints require `ROLE_MD`; there is no `ADMIN` role, and the `REPORTING` role does not grant administrative access. Dashboard and report endpoints retain their existing reader-role rules and must enforce department/project scope in the application layer.

# Reporting Service — Swagger UI and API Error Reference

## OpenAPI / Swagger URLs

When the service is reachable on its default local port:

- Swagger UI: **http://localhost:8092/swagger-ui/index.html**
- Swagger UI redirect: http://localhost:8092/swagger-ui.html
- OpenAPI JSON: http://localhost:8092/v3/api-docs
- OpenAPI YAML: http://localhost:8092/v3/api-docs.yaml

The OpenAPI document describes the reporting endpoints, JWT bearer authentication, common HTTP errors, and the `ApiProblem` response schema. Use **Authorize** in Swagger UI and enter the JWT as `Bearer <token>` (depending on the UI, the `Bearer` prefix may be added automatically; follow the prompt shown by the UI). Swagger/OpenAPI paths are public to load the documentation; reporting endpoints remain protected by the service security rules.

## Endpoint reference

All endpoints are relative to `/api/v1/reporting`. Responses are JSON unless an error is returned.

| Method | Path | Purpose | Access intent |
|---|---|---|---|
| GET | `/dashboard/summary` | KPI dashboard summary; optional `departmentId`, `projectId` | PROCUREMENT, TL, MD, REPORTING |
| GET | `/dashboard/freshness` | Latest observed event and ingestion freshness | PROCUREMENT, TL, MD, REPORTING |
| GET | `/reports/period-summary` | Aggregates for `period` and optional `from`, `to`, scope filters | Reporting roles permitted by security/scope policy |
| GET | `/reports/projections` | Pageable current projections; optional `entityType`, `status`, scope filters | Reporting roles permitted by security/scope policy |
| GET | `/reports/events` | Pageable retained event facts; optional `eventType`, ISO-8601 `from`/`to`, scope filters | Reporting roles permitted by security/scope policy |
| GET | `/reports/snapshots` | Pageable historical KPI snapshots | Reporting roles permitted by security/scope policy |
| POST | `/admin/snapshots` | Capture a point-in-time snapshot; optional ISO date `date` and scope filters | MD only, subject to scope policy |
| GET | `/admin/recovery-status` | Counts retained facts, projections, and snapshots | MD only |
| POST | `/admin/rebuild-projections?confirm=true` | Rebuild derived projections from retained event facts | MD only; explicit confirmation required |

Pagination defaults to `page=0`, `size=25`; size must be 1–200. Each list endpoint only accepts its documented allowlisted sort fields. `period` values are `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `HALF_YEARLY`, and `YEARLY`. Timestamp filters use ISO-8601 instants. Report data is eventually consistent.

## Error response format

API errors use `Content-Type: application/problem+json` and a stable body:

```json
{
  "type": "about:blank",
  "title": "Invalid reporting request",
  "status": 400,
  "detail": "Unsupported sort field: privateField",
  "instance": "/api/v1/reporting/reports/events",
  "code": "INVALID_REQUEST",
  "correlationId": "b87f2c2a-4e93-4cb6-a07c-8f99fbc4e3a0"
}
```

| HTTP | Code | Meaning / typical cause | Client action |
|---:|---|---|---|
| 400 | `INVALID_REQUEST` / `INVALID_PARAMETER` | Invalid filter, missing/invalid query parameter, invalid date range, unsupported sort field/direction, invalid enum | Correct the request; do not retry unchanged |
| 401 | `UNAUTHENTICATED` | Missing, expired, malformed, or invalid JWT | Obtain a valid access token and retry |
| 403 | `FORBIDDEN` | Role or department/project scope is not authorized | Check role and trusted scope claims; do not retry unchanged |
| 404 | Framework Problem Detail | Unknown route/resource | Check URL and API version |
| 405 | `METHOD_NOT_ALLOWED` | Unsupported HTTP method | Use the documented method |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Unsupported request content type | Use a supported content type |
| 500 | `INTERNAL_ERROR` | Unexpected server failure | Retry only if operation is safe; include correlation ID in support report |
| 503 | `REPORTING_DATA_UNAVAILABLE` | Reporting MongoDB data access failure | Retry with backoff and inspect service/database health |

Do not depend on exception stack traces or free-form `detail` text for program logic. Use HTTP status and `code`. A correlation ID is returned in the `X-Correlation-ID` response header and included in error bodies when available; provide it when troubleshooting. Server-side unexpected errors intentionally do not expose stack traces or database details.

## Quick verification (PowerShell)

```powershell
# Swagger UI is opened in a browser; this checks the OpenAPI JSON endpoint.
Invoke-RestMethod http://localhost:8092/v3/api-docs

# Unauthenticated API call should return HTTP 401 with application/problem+json.
try { Invoke-WebRequest http://localhost:8092/api/v1/reporting/dashboard/summary -ErrorAction Stop }
catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }

# Health and metrics endpoints (deployment security policy may restrict metrics externally).
Invoke-RestMethod http://localhost:8092/livez
Invoke-WebRequest http://localhost:8092/actuator/prometheus
```

Swagger/OpenAPI access is intentionally permitted by `SecurityConfig`; keep the actual business APIs protected. Review endpoint access rules against the platform's Auth Service role and scope claims before production release.
