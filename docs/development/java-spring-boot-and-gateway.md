# Java 21 + Production API Gateway

## Services

Every domain application is a standalone Spring Boot 3.x application using Java 21. Each service has its own `build.gradle`, `src/main/java`, `src/main/resources/application.yml`, and Dockerfile.

## Gateway responsibilities

The gateway is infrastructure-only. It provides:

- explicit allow-listed routes (service discovery locator disabled)
- JWT resource-server validation
- public login/refresh endpoints
- CORS policy
- security headers and HSTS
- correlation ID propagation
- Redis-backed per-client/user rate limiting
- circuit breakers and bounded downstream timeouts
- actuator health/metrics/prometheus endpoints
- environment-driven downstream URLs; no production service URLs are hardcoded

Authorization for domain operations remains the responsibility of each domain service.
