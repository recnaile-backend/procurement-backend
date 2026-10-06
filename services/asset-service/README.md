# Asset Service — Phase 1 Foundation

RECNAILE Asset Service foundation for reusable / asset-tracked items.

## Phase 1 scope

- Spring Boot 3.5.16
- Java 21
- Gradle build configuration
- MongoDB configuration using the `assetdb` database
- Actuator health/info and Kubernetes liveness/readiness probes
- Configurable service port (default `8089`)
- Environment-based MongoDB URI; no credentials are hard-coded
- Docker image definition
- Spring Boot context test

## Configuration

Default values:

```text
APP_SERVICE_NAME=asset-service
SERVICE_PORT=8089
MONGODB_DATABASE=assetdb
MONGODB_BASE_URI=mongodb://localhost:27017
```

For MongoDB Atlas, provide `MONGODB_BASE_URI` through the environment instead of putting credentials in source control.

## Build and run from the repository root

```powershell
./gradlew :services:asset-service:clean :services:asset-service:build
./gradlew :services:asset-service:bootRun
```

On Windows PowerShell, use `gradlew.bat` if required:

```powershell
./gradlew.bat :services:asset-service:clean :services:asset-service:build
./gradlew.bat :services:asset-service:bootRun
```

## Docker

Build the service from the repository root after the Gradle build:

```powershell
docker build -f services/asset-service/Dockerfile -t recnaile/asset-service:latest services/asset-service
```

Run it with environment variables supplied externally.

## Health endpoints

```text
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/info
```

## Important Asset ID convention for later phases

Asset IDs are server-generated. The agreed persisted/displayed format is:

```text
ASSET%2F26-27%2F001
ASSET%2F26-27%2F002
ASSET%2F26-27%2F003
```

`%2F` is intentionally part of the Asset ID value according to the current project requirement. The create API in later phases must not accept an `assetId` from the client.

## Later phases

This phase intentionally does not implement the Asset aggregate, assignment history, lifecycle state machine, REST APIs, Kafka consumers, idempotency, audit integration, or security/RBAC. Those are implemented in their respective phases.
