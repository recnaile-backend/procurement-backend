# Grafana Dashboards (provisioned as code)

Each `.json` file here is a Grafana dashboard model, auto-loaded via
`provisioning/dashboards.yaml`. Dashboards are edited via the dashboard-as-code workflow
(export from Grafana UI during development, commit the JSON, PR review) — never hand-edited
directly in a production Grafana instance.

## Dashboards

| File | Purpose |
|---|---|
| `platform-overview.json` | Golden signals (request rate, error rate, latency) across all services |
| `service-detail.json` | Per-service drill-down, parameterized by a `service` template variable |
| `kafka-health.json` | Consumer lag, DLT volume, rebalance frequency, partition health |
| `mongodb-health.json` | Connection pool usage, query latency, error/timeout rates |
| `kubernetes-cluster.json` | CPU/memory/pod-restart/node-health across the cluster |
| `business-kpis.json` | Procurement funnel and SLA-compliance metrics |

Placeholder dashboard JSON models are intentionally minimal here — each is fleshed out
with real panel queries as each service is implemented and its actual metric label sets
are finalized.
