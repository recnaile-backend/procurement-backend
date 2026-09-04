# Runbook: MongoDB Atlas Restore

1. In Atlas, identify the target snapshot (scheduled backup or a specific PITR timestamp)
   for the affected database's cluster.
2. Restore to a **new** cluster (never restore in-place over a live production cluster)
   to allow validation before cutover.
3. Validate data integrity against expected state (spot-check recent business records,
   confirm indexes exist per `docs/database/index-strategy.md`).
4. Update the affected service's connection string (via Secrets Manager → External
   Secrets Operator) to point at the restored cluster.
5. Roll the affected service's pods to pick up the new connection string.
6. Decommission the old cluster only after a documented soak period with no issues.
