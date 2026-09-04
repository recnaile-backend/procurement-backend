# Database-Per-Service

## 1. Rule

No microservice may connect to, query, or otherwise access another service's MongoDB
database. All cross-service data needs are satisfied either by:

1. Consuming the owning service's Kafka events (preferred, asynchronous), or
2. A synchronous REST call to the owning service's public API (only for the small,
   documented set of real-time checks, e.g., outward-service checking current available
   stock from inventory-service).

Never a shared connection string, shared credentials, or direct cross-database query.

## 2. Database Allocation

| Service | Database |
|---|---|
| auth-service | `authdb` |
| master-data-service | `masterdatadb` |
| indent-service | `indentdb` |
| po-service | `podb` |
| inward-service | `inwarddb` |
| inventory-service | `stockdb` |
| outward-service | `outwarddb` |
| return-service | `returndb` |
| asset-service | `assetdb` |
| notification-service | `notificationdb` |
| audit-service | `auditdb` |
| dashboard-service | `dashboarddb` |

## 3. Credentials & Access

- Each service has its own MongoDB Atlas database user, scoped via Atlas's built-in
  role-based access to **read/write on its own database only**.
- Credentials are stored in AWS Secrets Manager and synced into the service's own
  Kubernetes `Secret` via External Secrets Operator — never shared, never in
  `ConfigMap`s, never in Git.
- Network access: Atlas IP access list restricted to the VPC CIDR (via PrivateLink/VPC
  Peering) — no `0.0.0.0/0` entries in any environment, including dev.

## 4. Connection Pooling

- Standard MongoDB driver connection pool per service instance, sized to
  `maxPoolSize` proportional to expected concurrent request volume and tuned per service
  (documented per-service once implemented) — not left at a single global default across
  wildly different workloads (e.g., dashboard-service's read-heavy aggregation profile vs.
  indent-service's write-heavy transactional profile).
- Pool exhaustion is monitored (`mongodb_connections_current` vs. `mongodb_connections_max`)
  and alerted on before it becomes user-visible latency.

## 5. Read/Write Concerns

- Business-critical writes (indent state transitions, PO issuance, stock movements) use
  `writeConcern: majority` to ensure durability across the replica set before
  acknowledging.
- Read concern `majority` for reads that feed subsequent business decisions (e.g., reading
  current stock before an outward approval); `local` is acceptable for
  non-authoritative, latency-sensitive reads (e.g., dashboard aggregates that are already
  eventually consistent by nature).

## 6. Indexing Strategy

See `docs/database/index-strategy.md`.

## 7. Backup, Retention, DR

See `docs/disaster-recovery/disaster-recovery-plan.md`. Summary: MongoDB Atlas continuous
backups with point-in-time recovery, snapshot retention tuned per environment (shorter for
dev/staging, compliance-driven retention for production).

## 8. Encryption

- Encryption at rest: Atlas-managed encryption, optionally with a customer-managed KMS key
  for production.
- Encryption in transit: TLS enforced on all Atlas connections (default and non-negotiable).

## 9. Production Sizing Strategy

- Start on an Atlas dedicated cluster tier appropriate to each database's workload profile
  (e.g., `stockdb` and `indentdb` are write-heavy and latency-sensitive; `auditdb` and
  `dashboarddb` are append-heavy/read-heavy respectively) — sized and scaled
  independently since they are separate Atlas databases/clusters or, at minimum,
  independently monitored databases within a shared cluster in lower environments.
- Vertical scaling (cluster tier) and horizontal scaling (sharding) are both available
  per-database as growth requires; the database-per-service boundary makes this decision
  independent per domain.
