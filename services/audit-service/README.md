# RECNAILE Audit Service

This module belongs at `services/audit-service` in the existing RECNAILE Gradle multi-project repository. Preserve the repository root Gradle settings/build files, shared contracts, and root Docker Compose configuration. This ZIP intentionally contains exactly one Markdown file: `README.md`; phase notes are consolidated here.

## Implemented phases

- **Phase 01 — Domain model:** Typed `AUDIT_EVENT` envelope and explicit payload fields with contract tests.
- **Phase 02 — MongoDB persistence:** Append-only event documents, repository access, and history indexes.
- **Phase 03 — Kafka consumer:** JSON parsing, Bean Validation, manual acknowledgement after persistence, bounded retries, and dead-letter publishing.
- **Phase 04 — Idempotency:** Event UUID is the durable identity; exact duplicates are accepted and conflicting reuse is rejected.
- **Phase 05 — Application services:** Separate ingestion and read-only query use cases; Kafka adapter calls the ingestion service.
- **Phase 06 — Read-only REST API:** Versioned query endpoints, standardized success/error envelopes, offset pagination, fixed descending timestamp sort, request validation, controller tests, and OpenAPI documentation.

## Phase 06 API endpoints

Base path: `/api/v1/audit-events`

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/{eventId}` | Fetch one event by UUID; returns 404 when absent |
| GET | `/entities/{entityType}/{entityId}?page=0&size=20` | Entity history |
| GET | `/actors/{actorId}?page=0&size=20` | Actor history |
| GET | `/correlations/{correlationId}?page=0&size=20` | Correlation history |
| GET | `/traces/{traceId}?page=0&size=20` | Trace history |
| GET | `/services/{service}/actions/{action}?page=0&size=20` | Service/action history |

List endpoints return `data.content`, `data.page`, `data.size`, `data.totalElements`, and `data.totalPages` inside the platform success envelope. Page indexes are zero-based. Page size is limited to 1–100. Results are sorted by `timestamp` descending by the server; arbitrary client sort fields are not accepted.

Success envelope: `{"success":true,"data":{},"error":null}`. Error envelope: `{"success":false,"data":null,"error":{"code":"...","message":"...","traceId":null}}`.

## OpenAPI

- Runtime OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`
- Source contract: `src/main/resources/openapi/audit-service.openapi.yaml`

## Configuration

- Port: `SERVICE_PORT` (default `8091`).
- MongoDB base URI: `MONGODB_BASE_URI`; database: `MONGODB_DATABASE` (default `audit`).
- Kafka bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS` (default `kafka:29092`).
- Input topic: `KAFKA_AUDIT_TOPIC` (default `procurement.audit.events`).
- Consumer group: `KAFKA_CONSUMER_GROUP` (default `audit-service.audit-events`).
- Dead-letter topic: `<input-topic>.dlt`.

## Replace the module (PowerShell)

Run from the repository root. Commit or back up your current work before replacing the directory.

```powershell
Remove-Item -Recurse -Force .\services\audit-service
Expand-Archive .\audit-service-phase-06.zip -DestinationPath .\services -Force
```

## Verify tests and build

```powershell
.\gradlew.bat :services:audit-service:test
.\gradlew.bat :services:audit-service:build
```

## Start Docker Compose

```powershell
docker compose up --build -d
```

Confirm the Audit Service block is enabled in the root Compose file and that Kafka input and DLT topics exist. This module ZIP does not replace or modify the repository root Compose file.

## Security deployment gate

This phase adds read-only endpoints and API documentation but does not yet configure JWT resource-server authentication or role/entity-level authorization. **Do not expose or deploy these endpoints to a shared or production environment until Phase 07 security is implemented and verified.**

## Git commit summary

**Summary:** Added read-only, versioned audit query endpoints with bounded offset pagination, standard API envelopes, consistent error mapping, OpenAPI documentation, and controller tests. Preserved append-only audit behavior and one README file.

```powershell
git add services/audit-service
git commit -m "feat(audit-service): add read-only audit query API"
```

## Important behavior

- Audit events remain append-only; no update/delete endpoint is provided.
- MongoDB insertion must succeed before Kafka acknowledgement.
- Exact redelivery is idempotent; reuse of an event ID with different content is rejected.
- Run Gradle tests and build in the full repository before moving to the next phase. ZIP integrity checks alone do not prove compilation or runtime behavior.
