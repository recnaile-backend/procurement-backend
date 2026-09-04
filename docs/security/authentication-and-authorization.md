# Authentication & Authorization

## 1. Token Issuance

`auth-service` (business logic not implemented in this generation — contract fixed here)
issues:

- **Access token** — JWT, RS256-signed, short-lived (recommended 15 minutes).
- **Refresh token** — longer-lived, rotated on use, revocable (stored hashed, server-side).

JWT claims (minimum):

```json
{
  "sub": "userId",
  "roles": ["EMPLOYEE"],
  "departmentId": "dept-id",
  "iss": "auth-service",
  "iat": 0,
  "exp": 0,
  "jti": "token-id"
}
```

## 2. Gateway Responsibility

- Validates signature against the public key (fetched from `auth-service`'s JWKS endpoint,
  cached, rotated).
- Validates `exp`/`iss`/`nbf`.
- Rejects unauthenticated requests to protected routes with 401 before they ever reach a
  domain service.
- Does **not** make authorization decisions beyond "is this a valid, unexpired token" —
  role/entity-level authorization is a domain-service responsibility.

## 3. Domain Service Responsibility

Each domain service:

1. Re-validates the JWT (defense in depth — never assumes the gateway is the only entry
   point, especially inside a cluster where NetworkPolicies, not trust, are the real
   boundary).
2. Extracts `roles` and enforces method/endpoint-level RBAC (e.g., only `TL`/`MD` may call
   the approve endpoint, and further, `TL` only within the configured approval threshold).
3. Never accepts identity from a raw header (`X-User-Id`, `X-Roles`, etc.) unless that
   header was itself derived from a validated JWT at the gateway **and** the network path
   guarantees only the gateway can set it (defense-in-depth still re-validates the token
   itself where feasible).

## 4. Approval Threshold Rule (preserved business rule)

```text
Indent value below configured threshold  → requires TL approval
Indent value at/above configured threshold → requires MD approval
```

The threshold value is externalized configuration (not hardcoded), so it can be tuned
per environment/department without a code change.

## 5. Kubernetes RBAC Summary

| Subject | Scope |
|---|---|
| Each service's `ServiceAccount` | Read own `ConfigMap`/`Secret` in its namespace only |
| Argo CD `ServiceAccount` | Namespaced `Role` per managed namespace; no cluster-admin |
| External Secrets Operator | Read `Secret` create/update permission, namespaced |
| CI runner (GitHub Actions OIDC role) | ECR push only — **no** Kubernetes API access at all |

See `infrastructure/kubernetes/base/rbac/` for concrete manifests.
