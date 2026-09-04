# Disaster Recovery Plan

## 1. Objectives

| Component | RPO | RTO |
|---|---|---|
| MongoDB Atlas (per service DB) | ≤ 15 minutes (continuous backup + PITR) | ≤ 1 hour (restore to new cluster + repoint) |
| Kafka (event backbone) | 0 for durably-committed events (replication factor 3, `min.insync.replicas=2`) | ≤ 30 minutes (broker replacement / MSK failover) |
| S3 (documents) | 0 (versioning + cross-region replication for production) | ≤ 15 minutes (already durable/multi-AZ by design) |
| EKS / Kubernetes workloads | N/A (stateless, GitOps-defined) | ≤ 20 minutes (Argo CD re-sync to a healthy or new cluster) |
| Secrets (AWS Secrets Manager) | 0 (AWS-managed durability) | ≤ 10 minutes (External Secrets Operator re-sync) |
| GitOps repository | 0 (Git itself is the durable source of truth) | Immediate (any clone is a full recovery point) |
| ECR images | 0 (retained per retention policy, immutable tags) | Immediate (pull by tag) |

These are platform-level targets; specific environments may tighten them per SLA.

## 2. MongoDB Backup & Recovery

- Atlas continuous backups with point-in-time recovery enabled for all production
  databases.
- Backup snapshots retained per a documented schedule (e.g., hourly for 24h, daily for 7
  days, weekly for 4 weeks, monthly for 12 months in production; a shorter schedule in
  dev/staging).
- Recovery runbook: `docs/runbooks/mongodb-restore.md`.

## 3. S3 Recovery

- Versioning enabled on all buckets; accidental delete/overwrite is recoverable via
  version history.
- Cross-region replication enabled for the production documents bucket.
- Lifecycle rules transition older versions to cheaper storage classes rather than
  deleting them outright, within the compliance-driven retention window.

## 4. ECR Retention

- Immutable tags (`service:<git-sha>`) are never overwritten.
- Lifecycle policy retains all images referenced by any Argo CD environment's current or
  prior N revisions, and expires untagged/orphaned images after a documented window.

## 5. Kafka Recovery

- Replication factor 3 across AZs means single-broker loss requires no manual
  intervention (leader election handles it).
- Full-cluster-loss scenario (self-managed path): restore brokers from EBS snapshots
  (if snapshotting is enabled) or accept event-window data loss bounded by the last
  successfully replicated offset, then replay from source-of-truth MongoDB state where
  events are re-derivable, or from consumer-side reconciliation jobs.
- MSK path: leverages AWS-managed multi-AZ durability and standard MSK DR guidance.

## 6. Kubernetes / GitOps Recovery

- Because all workload definitions live in the GitOps repository, standing up a
  replacement EKS cluster is: provision cluster (Terraform) → install Argo CD → point
  Argo CD at the existing GitOps repo → let it reconcile. No manual manifest
  reconstruction is required.
- Secrets are re-synced automatically by External Secrets Operator once the new cluster's
  IRSA roles are provisioned.

## 7. Secrets Recovery

- AWS Secrets Manager is itself the durable, multi-AZ source of truth; nothing
  cluster-side needs separate backup.

## 8. Disaster Recovery Runbooks

- `docs/runbooks/mongodb-restore.md`
- `docs/runbooks/kafka-broker-recovery.md`
- `docs/runbooks/cluster-rebuild.md`
- `docs/runbooks/rollback.md`

## 9. Testing Cadence

- Full DR game day (simulated cluster loss + GitOps rebuild) at least twice per year in a
  non-production environment.
- MongoDB restore-from-backup tested quarterly.
- Results and any remediation items are tracked and closed before the next game day.
