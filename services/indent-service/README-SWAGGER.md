# Indent Service — Swagger / OpenAPI

## Swagger UI

Local Swagger UI:

`http://localhost:8083/swagger-ui/index.html`

OpenAPI document:

`http://localhost:8083/v3/api-docs`

## Authentication

Swagger/OpenAPI endpoints and actuator health endpoints are public. Business APIs remain protected by JWT and method-level RBAC.

Use **Authorize** in Swagger and provide the access token issued by Auth Service.

Supported roles:

- `EMPLOYEE`
- `PROCUREMENT`
- `TL`
- `MD`

## Documented business operations

- `POST /api/v1/indents` — Create and submit an indent (EMPLOYEE)
- `GET /api/v1/indents` — Get all indents; optional `departmentName` filter
- `GET /api/v1/indents/{indentNumber}` — Get indent details
- `POST /api/v1/indents/{indentNumber}/auto-verify` — Verify requested items against Master Data
- `POST /api/v1/indents/{indentNumber}/verify` — Procurement verification
- `POST /api/v1/indents/{indentNumber}/approve` — TL/MD approval
- `POST /api/v1/indents/{indentNumber}/reject` — TL/MD rejection

The controller annotations describe request parameters, roles, workflow transitions, idempotency behavior, and expected HTTP responses in the generated OpenAPI document.
