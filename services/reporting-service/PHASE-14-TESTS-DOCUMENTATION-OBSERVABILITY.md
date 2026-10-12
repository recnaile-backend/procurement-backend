# Phase 14 — Tests, Documentation and Observability

## Implemented

- Added an HTTP correlation-ID filter. A safe incoming `X-Correlation-ID` is retained; missing or unsafe values are replaced with a generated UUID. The identifier is returned in the response and added to SLF4J MDC, then cleared at request completion.
- Added collection gauges for retained facts, current projections and historical snapshots.
- Added Kafka message-received and processing-failure counters plus a consume-duration timer split only by the low-cardinality `outcome` tag.
- Added a log-level pattern that includes service name, trace ID, span ID and correlation ID. Existing Micrometer Prometheus registry, OpenTelemetry tracing bridge, OTLP exporter and Actuator endpoints remain enabled.
- Added tests covering safe correlation-ID propagation, generated IDs for unsafe input and MDC cleanup. Existing model, scope, event, dashboard and recovery tests remain in place.

## Verification

Run from the repository root:

```powershell
.\gradlew :services:reporting-service:clean :services:reporting-service:test
.\gradlew :services:reporting-service:build
docker compose build reporting-service
docker compose up -d reporting-service
docker compose ps reporting-service
Invoke-RestMethod http://localhost:8092/actuator/health
Invoke-WebRequest http://localhost:8092/actuator/prometheus
```

If using `compose-debug.yml`, add `-f compose-debug.yml` to each Docker Compose command.

## Metrics and endpoints

- `GET /actuator/health` — overall health.
- `GET /actuator/health/liveness` and `/actuator/health/readiness` — probe endpoints when enabled by Spring Boot.
- `GET /actuator/prometheus` — Prometheus scrape output.
- `GET /actuator/metrics/reporting.events.processed` — event processing counters (when at least one event has been processed).
- `GET /actuator/metrics/reporting.kafka.consume` — Kafka processing timer.
- `GET /actuator/metrics/reporting.facts.count` — retained fact count gauge.
- `GET /actuator/metrics/reporting.projections.count` — projection count gauge.
- `GET /actuator/metrics/reporting.snapshots.count` — snapshot count gauge.
- Send `X-Correlation-ID: demo-123` to an API request and verify that the response includes the same header and logs contain the correlation ID. Invalid incoming IDs are replaced.

## Operations and security notes

- Configure Prometheus to scrape `/actuator/prometheus` and route OTLP traces to the configured collector endpoint. Existing platform Grafana/Loki/Tempo provisioning remains a platform responsibility; this service package does not assume dashboard IDs or credentials.
- Do not add department IDs, project IDs, event IDs, or user identifiers as metric tags; that would create high-cardinality metrics and could expose sensitive operational identifiers.
- Ensure the Prometheus endpoint is reachable only from the trusted monitoring network. Keep secrets in environment variables or a secrets manager, not source control or log output.
- Kafka dead-letter topics must exist with compatible partitioning. Alert on processing failures and DLT growth; verify this in the deployment environment.
