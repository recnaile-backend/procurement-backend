# Inward Service — Phase 14 Observability

## Metrics

Actuator exposes Prometheus-compatible metrics at `/actuator/prometheus`.
Spring Boot/Micrometer provides HTTP, JVM, MongoDB and Kafka metrics where applicable.
The service also exposes low-cardinality custom metrics:

- `inward_http_requests_total` (Prometheus counter suffix)
- `inward_http_request_duration`
- `inward_grn_created_total` (Prometheus counter suffix)
- `inward_outbox_events_created_total` (Prometheus counter suffix)
- `inward_outbox_events_published_total` (Prometheus counter suffix)
- `inward_outbox_events_failed_total` (Prometheus counter suffix)
- `inward_audit_events_created_total` (Prometheus counter suffix)

## Tracing

Micrometer Tracing/OpenTelemetry exports traces to the configured OTLP endpoint.
`TRACING_SAMPLE_RATE` controls sampling. The default is `0.1`.

## Correlation

`X-Correlation-Id` is accepted from callers and generated when absent. It is returned on the response and placed into logging MDC. W3C `traceparent` is supported by the tracing stack; trace and span identifiers are included in structured logs when available.

## Logs

Container console logs are JSON with timestamp, level, service, logger, traceId, spanId and correlationId fields. Secrets and JWT values are not logged by this observability layer.

## Health

- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/metrics`
- `/actuator/prometheus`

The existing Docker healthcheck uses the liveness endpoint.

## Grafana stack

Prometheus scrapes `/actuator/prometheus`. Grafana can use Prometheus for metrics, Loki for logs and Tempo for traces. OTLP export is configurable with `OTEL_EXPORTER_OTLP_ENDPOINT` or `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`.
