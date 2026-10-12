# Phase 15 — Platform Integration and Release

## Included release changes

- `GET /livez` is a process-liveness probe and is the Docker health-check target.
- `GET /readyz` checks application readiness and MongoDB connectivity.
- Public health endpoints do not disclose component details.
- `/actuator/info` reports the service name, description, version, and release track. Configure `REPORTING_SERVICE_VERSION` and `REPORTING_RELEASE_TRACK` through deployment environment variables.
- Graceful shutdown is enabled with a 20-second phase timeout.
- Root-platform integration still must be checked in the actual repository: Docker Compose networking, Kafka bootstrap address and topic regex, MongoDB database/credentials, Auth Service JWT policy, Prometheus scrape target, and OTLP endpoint.

## Release checklist

- [ ] Root Gradle clean test and build succeed.
- [ ] Docker image builds and container remains healthy.
- [ ] `/livez` and `/readyz` return HTTP 200; stop MongoDB in a test environment and confirm readiness fails.
- [ ] `/actuator/info` returns the expected release version and release track.
- [ ] `/actuator/prometheus` is scrapeable by the platform Prometheus instance.
- [ ] A test JWT from the platform Auth Service can access a permitted report; a role without report access is denied.
- [ ] Publish/replay a representative event to a topic matched by `REPORTING_TOPIC_PATTERN`; verify a retained fact/projection, Kafka logs, and metrics.
- [ ] Verify the Kafka dead-letter topics exist and are monitored according to the root platform's topic/partition policy.
- [ ] Confirm no credentials or signing secrets are committed in source, Compose files, logs, or this package.
- [ ] Back up MongoDB and document the recovery procedure before production rollout.

## Repository-root verification (PowerShell)

```powershell
.\gradlew :services:reporting-service:clean :services:reporting-service:test
.\gradlew :services:reporting-service:build
docker compose build reporting-service
docker compose up -d reporting-service
docker compose ps reporting-service
Invoke-RestMethod http://localhost:8092/livez
Invoke-RestMethod http://localhost:8092/readyz
Invoke-RestMethod http://localhost:8092/actuator/info
Invoke-WebRequest http://localhost:8092/actuator/prometheus
docker compose logs --tail=200 reporting-service
```

If the project uses `compose-debug.yml`, add `-f compose-debug.yml` to each Docker Compose command. The script below checks local HTTP endpoints and reports Docker state, but it cannot replace authenticated API and real Kafka-event verification.


## API documentation and error contracts

- Swagger UI: `http://localhost:8092/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8092/v3/api-docs`
- Error contract and status mapping: `docs/API-AND-ERROR-DOCUMENTATION.md`
- Business API errors and JWT 401/403 responses use `application/problem+json` with a stable `code` and correlation identifier.


### Numeric API values

Reporting quantities, estimated values, unit prices, stock thresholds, and SLA hour values are exposed as whole-number integers. Incoming numeric values containing a fractional component are rounded to the nearest integer during event ingestion.
