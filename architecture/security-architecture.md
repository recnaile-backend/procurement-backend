# Security Architecture

## 1. Identity & Access

- **AuthN model:** OAuth2/OIDC-compatible. `auth-service` (unimplemented business logic,
  but its contract is fixed here) issues JWTs signed with **RS256**.
- **Token validation:** the API Gateway validates JWT signature, expiry, and issuer on
  every request before routing. This is authentication only.
- **Authorization:** each domain service independently re-validates the caller's role and
  entity-level permissions from **verified JWT claims** — the gateway's authentication is
  never treated as sufficient authorization by a downstream service, and no service trusts
  arbitrary user-supplied identity headers (e.g., a raw `X-User-Id` header without a valid
  signed token backing it).

### Roles

```text
EMPLOYEE     — submit indents, submit outward requests
PROCUREMENT  — verify indents, manage POs, inward/GRN
TL           — approve indents below configured threshold
MD           — approve indents above configured threshold
ADMIN        — master data, user/role management
```

## 2. Transport Security

- TLS terminates at the ALB (AWS Certificate Manager-issued certificate) and is
  re-encrypted (or passed through an internal cert) to the Gateway.
- mTLS is used for gateway → domain-service traffic where the ingress/mesh layer supports
  it, and is mandatory for any traffic crossing the VPC boundary (e.g., to MongoDB Atlas,
  which itself requires TLS).

## 3. AWS IAM

- Each EKS workload uses **IRSA (IAM Roles for Service Accounts)** — no static AWS
  credentials are ever mounted into a pod.
- Least-privilege IAM policies scoped per service: e.g., only the service that writes
  invoice documents (inward-service, when implemented) gets `s3:PutObject`/`s3:GetObject`
  on its specific prefix — not bucket-wide access.

## 4. Kubernetes RBAC

- Each service has its own `ServiceAccount`.
- `Role`/`RoleBinding` grants are namespaced and scoped to only the resources a workload's
  own controllers need (e.g., reading its own `ConfigMap`/`Secret`) — no `ClusterRole`
  grants for domain workloads.
- Cluster-level RBAC (e.g., for Argo CD, cert-manager, external-secrets) is restricted to
  the platform/infra namespaces that need it.

## 5. Secrets Management

```text
AWS Secrets Manager
        │
        ▼
External Secrets Operator (polls/syncs on interval)
        │
        ▼
Kubernetes Secret (namespace-scoped)
        │
        ▼
Application (mounted as env var / file — never logged)
```

- No secret, password, private key, API key, or credential is ever committed to Git.
- `.gitignore` and pre-commit/CI secret scanning (gitleaks/TruffleHog via GitHub Actions)
  enforce this.

## 6. Perimeter Security

- **AWS WAF** in front of the ALB: managed rule groups (SQLi, XSS, known bad inputs) plus a
  rate-based rule for basic DDoS/brute-force mitigation.
- **Gateway rate limiting** (Redis-backed token bucket via Spring Cloud Gateway
  `RequestRateLimiter`) as a second, application-aware layer.
- **Security headers** applied at the gateway: `Strict-Transport-Security`,
  `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy` (API-appropriate),
  `Referrer-Policy`.

## 7. Data Protection

- **At rest:** MongoDB Atlas encryption at rest (AES-256, Atlas-managed or customer KMS
  key); S3 buckets use SSE-KMS with an AWS KMS customer-managed key; EBS volumes for any
  stateful workloads (Kafka, if self-managed) are encrypted.
- **In transit:** TLS everywhere — client↔ALB, ALB↔gateway, gateway↔services,
  services↔MongoDB Atlas, services↔Kafka (TLS + SASL).
- **Documents:** invoices/attachments stored only in S3; MongoDB stores the S3 object key,
  content type, size, and a presigned-URL-generation reference — never the binary.

## 8. Audit Logging

- Every create/update/approve/reject/delete/deactivate business action emits an audit
  event, consumed by `audit-service` into an **append-only** collection — no update or
  delete API is exposed for audit records at the application layer.
- Audit records capture `service`, `entityType`, `entityId`, `action`, `performedBy`,
  `before`, `after`, `timestamp`, `traceId`, `correlationId`.

## 9. Threat Model Summary

| Threat | Mitigation |
|---|---|
| Token forgery | RS256 signature validation, short-lived access tokens, refresh rotation |
| Privilege escalation via headers | Gateway strips/ignores client-supplied identity headers; only verified JWT claims are trusted |
| Cross-service data access | Database-per-service; no shared MongoDB credentials |
| Secret leakage | External Secrets Operator + Secrets Manager; secret scanning in CI |
| DDoS / brute force | WAF rate-based rules + gateway rate limiting |
| Event replay / duplication | Idempotent consumers keyed on `eventId` (see event-driven-architecture.md) |
| Lateral movement inside cluster | Default-deny NetworkPolicies, namespaced RBAC |
| Supply-chain (dependency/image) | Trivy container scan, GitHub dependency scanning, OWASP dependency check in CI |
