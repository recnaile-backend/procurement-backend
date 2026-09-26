# Outward Service — Phase 01

## Foundation & Service Setup

This phase establishes the Outward Service foundation only. No outward business
workflow, domain state machine, approval logic, issue transaction, Kafka business
event, idempotency implementation, or inventory mutation is implemented here.

### Service identity

- Service: `outward-service`
- Package root: `com.recnaile.outward`
- Port: `8087`
- MongoDB database: `outward`
- Public API base reserved for later phases: `/api/v1/outward/**`
- Kafka bootstrap server: environment-configured

### Platform foundation

- Java 21
- Spring Boot / Gradle multi-project conventions
- MongoDB Atlas/service-owned database configuration
- Kafka bootstrap configuration
- Actuator health/metrics/Prometheus endpoints
- OpenTelemetry OTLP exporter configuration
- Micrometer tracing bridge
- Spring Security Resource Server and OAuth2 JWT dependencies for the later security phase
- SpringDoc/OpenAPI foundation
- Docker build/runtime definition

### Phase boundary

Business endpoints are intentionally not implemented in Phase 01. Later phases
will add the explicit domain model, state machine, authorization, APIs,
idempotency/concurrency, transactional outbox, Kafka events, audit,
notifications, observability details and tests required by the outward workflow.

### Verification

From the repository root:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:test
```

For a complete service build:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:build
```
