# Runbook: Kafka Broker Recovery

## Single Broker Loss

With replication factor 3 and `min.insync.replicas=2`, a single broker loss is handled by
automatic leader election — no immediate manual action required beyond replacing the failed
broker (MSK: automatic; self-managed/Strimzi: the Operator reschedules the pod, which
rejoins and re-syncs from replicas).

## Multi-Broker / AZ Loss

1. Confirm remaining broker count still satisfies `min.insync.replicas` for affected
   partitions; if not, producers will see `NotEnoughReplicasException` — this is
   expected, safe backpressure, not data loss.
2. Restore/replace brokers (MSK: AWS-managed; self-managed: Strimzi Operator + underlying
   EKS node group scale-out).
3. Once brokers rejoin and under-replicated-partition count returns to zero, resume normal
   operations.
4. Cross-check consumer lag dashboards post-recovery to confirm consumers are catching up
   and not stuck.
