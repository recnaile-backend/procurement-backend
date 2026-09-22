# 401 troubleshooting update

This version keeps the intended authorization model and makes JWT authentication failures observable.

- GET `/api/v1/indents/{indentNumber}` remains available to any authenticated user.
- POST `/api/v1/indents/{indentNumber}/verify` remains restricted to `PROCUREMENT`.
- Explicit `bearerAuth` security requirements are present on protected controller operations so Swagger/OpenAPI documents them consistently.
- The resource-server authentication entry point logs the exact request URI and JWT authentication exception. A 401 is generated before `@PreAuthorize`; a role failure remains 403.
- Encoded slash support remains enabled because the canonical indent number is literally stored as `IN%2FYY-YY%2FNNN`.

After deploying, reproduce the 401 and inspect:

```powershell
docker compose logs -f --since=10s indent-service
```

Look for:

`JWT authentication failed | method=... | uri=... | reason=...`

Do not weaken the PROCUREMENT role to fix a 401. If the token is authenticated but lacks the role, the endpoint should return 403.
