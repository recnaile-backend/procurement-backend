# Local Verification SOP

## Gate 1 - Tooling

```powershell
java -version
docker --version
docker compose version
gradle --version
```

Java must be 21.

## Gate 2 - Infrastructure

```powershell
docker compose up -d
docker compose ps
```

Verify Redis:

```powershell
docker exec redis redis-cli ping
```

Verify Tempo:

```powershell
curl http://localhost:3100/ready
```

Verify Prometheus:

```text
http://localhost:9090
```

Verify Grafana:

```text
http://localhost:3000
```

## Gate 3 - Build

```powershell
gradle clean build
```

Do not continue if the build fails.

## Gate 4 - Gateway

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
gradle :gateway:api-gateway:bootRun
```

In another terminal:

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/platform/ping
```

## Gate 5 - Docker

```powershell
docker compose --profile app up -d --build
curl http://localhost:8080/actuator/health
```

## Gate 6 - AWS

Only after all local gates pass:

```powershell
aws sts get-caller-identity
terraform init
terraform validate
terraform plan
```

Never run production apply without an approved change/release.
