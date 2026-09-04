# Performance Tests (k6)

Load and soak scripts validating the platform under realistic traffic before a
production promotion (referenced by `.github/workflows/release.yml`'s staging-soak
gate), and periodically re-run against production in a controlled maintenance window.

## Scripts

| Script | Purpose |
|---|---|
| `indent-submission-load.js` | Ramping load on the indent submission endpoint; validates P95/P99 latency thresholds |
| `kafka-throughput-soak.js` | Sustained moderate load over 30 minutes; validates Kafka consumer lag stays bounded |

## Running

```bash
k6 run -e BASE_URL=https://staging.procurement.example.com \
       -e E2E_ACCESS_TOKEN="$(./scripts/mint-test-token.sh)" \
       indent-submission-load.js
```

Thresholds are set to fail the k6 run (non-zero exit) if P95/P99 latency or error rate
breach the same limits configured in `infrastructure/monitoring/prometheus-rules.yaml`,
keeping the two sources of truth aligned.
