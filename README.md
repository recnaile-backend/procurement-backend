# Procurement Platform Foundation

A production-oriented Spring Boot 3 / Java 21 platform foundation containing:

- Reactive Spring Cloud API Gateway
- Configurable JWT security
- Redis-backed rate limiting
- Correlation IDs
- Resilience4j circuit-breaker support
- Actuator + Prometheus metrics
- OpenTelemetry / Tempo integration
- Local MongoDB, Kafka, Redis and observability stack
- Docker build
- GitHub PR/CI/security workflows
- Terraform/Kubernetes/Helm foundation
- No business microservices included

## Important

This repository intentionally contains **zero business microservices**. It is a foundation repository. Business services can be added later using a standardized template.

## Quick start - Windows

1. Install Java 21 and Docker Desktop.
2. Install Gradle 8.14.5 (or use the Gradle wrapper after generating it).
3. Open PowerShell in the repository root.
4. Start infrastructure:

```powershell
docker compose up -d
```

5. Verify:

```powershell
docker compose ps
docker exec redis redis-cli ping
curl http://localhost:3100/ready
```

6. Build:

```powershell
gradle clean build
```

7. Start the gateway:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
gradle :gateway:api-gateway:bootRun
```

8. Verify:

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/platform/ping
```

Expected ping response includes:

```json
{"service":"api-gateway","status":"UP"}
```

## Docker application mode

```powershell
docker compose --profile app up -d --build
curl http://localhost:8080/actuator/health
```

## Stop

```powershell
docker compose --profile app down
```

## Production security

`dev` disables JWT validation so the gateway can be verified without an identity provider.

`staging` and `production` require:

```text
JWT_ISSUER_URI
```

Do not disable security in production.

## Environments

```text
AWS Account
├── DEV EKS
├── STAGING EKS
└── PRODUCTION EKS
```

Use the deployment scripts only after local verification and Terraform plan review.


## Database Standard

This foundation uses **MongoDB Atlas**. Local MongoDB is intentionally not included in
`docker-compose.yml`. Configure `MONGODB_URI` through environment variables for development
and through AWS Secrets Manager/External Secrets for AWS environments.

## Java Standard

This repository is **Java 21 only**. It contains no Kotlin source files and no Kotlin Gradle
plugin. Use the Gradle wrapper:

```powershell
.\gradlew.bat clean build
```

For the Gateway:

```powershell
.\gradlew.bat :gateway:api-gateway:bootRun
```


## Java 21 + MongoDB Atlas

This foundation is Java 21 only. MongoDB is not containerized; application services use `MONGODB_URI` to connect to MongoDB Atlas.

### Windows

1. Install JDK 21 and confirm `java -version` reports 21.
2. Copy `.env.example` to `.env` and set your MongoDB Atlas connection string.
3. Start infrastructure: `docker compose up -d`.
4. Build: `gradlew.bat clean build`.
5. Start the gateway: `gradlew.bat :gateway:api-gateway:bootRun`.
6. Verify: `curl http://localhost:8080/actuator/health`.

The Docker Compose file starts the gateway together with local infrastructure. MongoDB Atlas remains external.

## Security and code quality pipeline

The GitHub Actions security workflow intentionally combines multiple controls:

- **SonarQube** for static analysis, vulnerabilities, security hotspots, code smells and test coverage.
- **Trivy filesystem** scanning for dependency, secret and infrastructure-as-code findings.
- **Trivy container** scanning of the gateway image produced from the Dockerfile.
- **Gitleaks** for committed-secret detection.

See `docs/security/sonarqube-trivy-ci.md` for the required GitHub secrets, SonarQube setup and Quality Gate policy.

### GitHub Actions security gates

Before enabling the `Security & Code Quality` workflow on the protected branches, configure:

```text
Repository Settings -> Secrets and variables -> Actions

Secrets:
  SONAR_HOST_URL
  SONAR_TOKEN
  Gitleaks license (action-only; not required by the CLI)   (required for organizations using Gitleaks Action)

Variables:
  SONAR_PROJECT_KEY      (optional; default procurement-platform-foundation)
  SONAR_PROJECT_NAME     (optional; default Procurement Platform Foundation)
```

The workflow intentionally fails if SonarQube credentials are missing. This prevents a false-green security check.


## Business microservices

The repository now contains 12 Spring Boot 3.5 / Java 21 business services under `services/`, plus the API Gateway under `gateway/api-gateway/`. The split follows the supplied Procurement & Inventory Management specification: master data, indent, PO, inward/GRN, inventory, outward, return, asset, dashboard/reporting, notifications, audit and user/role management. fileciteturn0file0L65-L75

See `docs/MICROSERVICE-ARCHITECTURE.md` for the service map and workflow.

### Local startup

1. Copy `.env.example` to `.env` and set the Atlas credentials.
2. Run `docker compose up --build -d`.
3. Gateway: `http://localhost:8080`.
4. Kafka UI: `http://localhost:8095`.
5. Grafana: `http://localhost:3000`.

MongoDB is MongoDB Atlas; no local MongoDB container is used. Each business service uses its own logical database in the shared Atlas cluster.

### Security

`.env` is intentionally git-ignored. Do not commit credentials. The provided Atlas password should be rotated if it has been exposed in a repository, log, screenshot, or chat.
