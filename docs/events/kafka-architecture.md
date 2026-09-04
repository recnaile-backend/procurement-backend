# Kafka Architecture — Operational Guide

See `architecture/event-driven-architecture.md` for the design rationale. This document is
the operational reference.

## 1. Cluster Sizing (Production Guidance)

- Minimum 3 brokers (one per AZ) for production, replication factor 3, `min.insync.replicas=2`.
- Deployable as Amazon MSK (recommended for reduced operational burden) or self-managed via
  the Strimzi Kafka Operator on EKS — both are supported by this platform's Kubernetes/Helm
  layout; the choice is an environment-level Terraform variable
  (`infrastructure/terraform/modules/kafka`).
- `infrastructure/kafka/` holds broker configuration, topic definitions, and (for the
  self-managed path) Strimzi custom resources.

## 2. Topic Configuration

Authoritative definitions: `infrastructure/kafka/topics.yaml`. Standard defaults unless
overridden per topic:

| Setting | Default |
|---|---|
| Partitions | 6 |
| Replication factor | 3 |
| `min.insync.replicas` | 2 |
| `retention.ms` | 7 days (604800000) |
| `cleanup.policy` | `delete` |
| DLT topic retention | 30 days |

## 3. Producer Configuration Standard

```text
acks=all
enable.idempotence=true
retries=Integer.MAX_VALUE (bounded by delivery.timeout.ms)
max.in.flight.requests.per.connection=5   # safe with idempotence enabled
compression.type=snappy
```

## 4. Consumer Configuration Standard

```text
enable.auto.commit=false          # manual commit after successful processing
isolation.level=read_committed
max.poll.records=<tuned per service throughput>
session.timeout.ms / heartbeat.interval.ms=<cluster defaults>
```

## 5. Consumer Group Naming

`<consuming-service>.<topic-short-name>` — e.g. `inventory-service.inward-events`.
Never share a consumer group across two different services.

## 6. Local Development

Local Kafka runs via `docker-compose.yml` (single broker + Kafka UI). See
`docs/runbooks/local-development.md` and `infrastructure/kafka/local-topics-init.sh` for
topic bootstrap.

## 7. Retry & DLT

Each primary topic has a paired `.retry` and `.dlt` topic (see
`architecture/event-driven-architecture.md` §9). Retry topic consumers apply exponential
backoff with jitter; after a configured max-retry count, the message is written to `.dlt`
with failure metadata headers.

## 8. Replay Procedure

See `docs/runbooks/kafka-dlt-replay.md` — replay is always a deliberate, audited operator
action, never an automatic DLT-to-primary requeue.

## 9. Monitoring

Consumer lag, DLT volume, producer/consumer error rates, and rebalance frequency are
scraped via the Kafka Exporter (`infrastructure/monitoring/kafka-exporter.yaml`) into
Prometheus, visualized on the "Kafka health" Grafana dashboard, and alerted on per
`infrastructure/monitoring/alertmanager-config.yaml`.
