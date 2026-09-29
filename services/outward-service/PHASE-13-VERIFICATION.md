# Phase 13 — Observability Verification

## Scope

- Spring Boot Actuator endpoints for health, metrics and Prometheus.
- Micrometer observations for core outward operations.
- OpenTelemetry tracing bridge and OTLP export configuration.
- W3C trace propagation.
- Kafka producer observations enabled.
- HTTP correlation ID generation/propagation through MDC.
- Console logging includes trace, span and correlation identifiers.
- No bearer tokens or secrets are added to observability logs.

## Key endpoints

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/metrics`
- `/actuator/prometheus`

## Key observations

- `outward.request.create`
- `outward.request.submit`
- `outward.request.approve`
- `outward.request.reject`
- `outward.request.issue`
- `outward.master-data.validate`
- `outward.master-data.item`
- `outward.master-data.departments`
- `outward.master-data.project`
- `outward.outbox.publish.batch`

## Verification commands

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:test
.\gradlew :services:outward-service:clean :services:outward-service:build
```
