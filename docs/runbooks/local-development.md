# Runbook: Local Development

## 1. Prerequisites
- Docker + Docker Compose
- JDK 21, Java toolchain (for when a domain service is implemented locally)

## 2. Start Infrastructure

```bash
docker compose up -d
```

This brings up: MongoDB (single-node replica set for transaction support), Kafka + Kafka
UI, Redis, OpenTelemetry Collector, Prometheus, Grafana, Loki, Tempo.

## 3. Access Points

| Tool | URL |
|---|---|
| Kafka UI | http://localhost:8085 |
| Grafana | http://localhost:3000 (default admin/admin — change immediately) |
| Prometheus | http://localhost:9090 |
| MongoDB Atlas | `${MONGODB_URI}` |
| Redis | localhost:6379 |
| OTel Collector (OTLP gRPC) | localhost:4317 |

## 4. Bootstrap Kafka Topics

```bash
bash infrastructure/kafka/local-topics-init.sh
```

## 5. Point a Locally-Run Service at This Infrastructure

Once a domain service is implemented, its `application-local.yml` should point at:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI} (MongoDB Atlas connection string)
  kafka:
    bootstrap-servers: localhost:9092
  redis:
    host: localhost
    port: 6379
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
```

## 6. Tear Down

```bash
docker compose down -v   # -v also removes local volumes/data
```


## MongoDB Atlas

MongoDB is not started by Docker Compose in this foundation. Use a MongoDB Atlas cluster for
development, staging, and production.

Set the connection string outside source control, for example:

```powershell
$env:MONGODB_URI="mongodb+srv://<user>:<password>@<cluster>.mongodb.net/<database>?retryWrites=true&w=majority"
```

Never commit the real URI, username, password, API key, or certificate to Git. In AWS, inject
the value through AWS Secrets Manager / External Secrets rather than a Git-tracked `.env` file.
