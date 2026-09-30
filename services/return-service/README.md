# Return Service

Phase 1 — Foundation & Service Setup for the RECNAILE Procurement Platform.

## Service

- Service name: `return-service`
- Port: `8088`
- Public route: `/api/v1/returns/**` through the API Gateway
- Database: MongoDB Atlas, database `return`
- Messaging: Kafka
- Security foundation: Spring Security + OAuth2 Resource Server dependencies
- Observability foundation: Actuator, Prometheus metrics and OpenTelemetry/Micrometer tracing

## Phase 1 scope

This phase establishes the Spring Boot service foundation, environment-based configuration,
Actuator health/metrics, MongoDB/Kafka integration points, security dependencies and a context
startup test.

The previous generic CRUD `RecordDocument`/`RecordRepository`/controller scaffold has been removed.
Business domain models, state transitions, APIs, Kafka events, outbox, authorization rules and
cross-service integrations are implemented in subsequent phases.

## Configuration

Production/deployment configuration must provide `MONGODB_BASE_URI`, `MONGODB_DATABASE`,
`KAFKA_BOOTSTRAP_SERVERS`, `SERVICE_PORT`, `TRACING_SAMPLE_RATE`, and
`OTEL_EXPORTER_OTLP_ENDPOINT` through the existing environment/secret-management mechanism.
No credentials are stored in source control.

For local development, MongoDB and Kafka URI defaults are provided only as non-secret developer
fallbacks; production should always supply the platform-managed values.

## Verification

From the repository root:

```bash
./gradlew :services:return-service:clean :services:return-service:test
./gradlew :services:return-service:build
```

The service exposes standard Actuator endpoints under `/actuator`, including health, metrics and
Prometheus endpoints according to the platform configuration.
