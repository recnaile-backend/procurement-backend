# Reporting Service

Read-optimized reporting boundary for the RECNAILE Procurement Platform. It consumes procurement domain events from Kafka and maintains its own MongoDB projections in `reporting` (or `MONGODB_DATABASE`). It does not query operational service databases.

## APIs

- `GET /api/v1/reporting/dashboard/summary` — operational KPI counts.
- `GET /api/v1/reporting/dashboard/freshness` — latest observed event/update time and event count.
- `GET /api/v1/reporting/reports/period-summary?period=DAILY|WEEKLY|MONTHLY|QUARTERLY|HALF_YEARLY|YEARLY&from=YYYY-MM-DD&to=YYYY-MM-DD` — event counts and quantity totals for UTC date boundaries.
- `GET /api/v1/reporting/reports/projections?page=0&size=25&sort=updatedAt,desc` — pageable projection listing with entityType/status/departmentId/projectId filters.
- `GET /api/v1/reporting/reports/events` — pageable retained event facts; optional eventType/from/to/departmentId/projectId filters.
- `GET /api/v1/reporting/reports/snapshots` — pageable historical daily KPI snapshots.

All API endpoints require a JWT. The service validates HS256 tokens using `JWT_SECRET_KEY`, matching the Auth Service and existing resource-server configuration in the supplied repository. The secret must be at least 32 UTF-8 bytes. Confirm the exact issuer/validator policy with the platform security standard before release. Role claims `roles` or `authorities` are mapped to `ROLE_*`. Dashboard and detailed-report role requirements are declared in `config/SecurityConfig.java`. Verify claim shape against the platform Auth Service before release.

## Event ingestion

A configurable Kafka topic regex (`REPORTING_TOPIC_PATTERN`) selects platform event topics. Messages must use the platform envelope (`eventId`, `eventType`, `eventVersion`, `occurredAt`, `producer`, `payload`). Immutable facts are keyed by `eventId`, and current entity projections are upserted by `entityType:entityId`; replaying a duplicate event therefore does not create duplicate facts or additive counts. Unsupported event types are retained as `UNMAPPED` facts and do not create current projections.

## Consistency and rebuild

Reports are eventually consistent. Freshness endpoints make the latest observed event and ingestion time visible. Rebuild procedure: stop or isolate the reporting consumer; clear `reporting_projections` and `reporting_event_facts` only in the target reporting database; reset the reporting consumer group to the beginning of retained events (or use an approved event replay); restart and reconcile counts. Do not run this against production without an approved backup and cutover plan. Events older than Kafka retention require an approved source-data export/replay mechanism.

## Operational configuration

- `MONGODB_BASE_URI`, `MONGODB_DATABASE`
- `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONSUMER_GROUP`, `REPORTING_TOPIC_PATTERN`
- `JWT_SECRET_KEY` (HS256 shared secret, at least 32 bytes), `SERVICE_PORT`, `OTEL_EXPORTER_OTLP_ENDPOINT`

Stock limitation: the supplied contracts include `LOW_STOCK_ALERT` but no authoritative full-stock snapshot/change event. Consequently, `lowStockItemsObserved` and `availableQuantityAtLowStockAlerts` represent items observed in low-stock alert events, not the complete inventory balance. Add/approve a stock snapshot or stock-movement event contract before exposing a total current stock balance.

MongoDB indexes are ensured at startup for event type/time, entity/time, status/update time, and department/project/update time. Prometheus counters include processed, duplicate, and failed events. Failed messages are retried and then published to `<source-topic>.DLT`; ensure corresponding DLT topics exist with compatible partition counts and monitor them operationally.

## Important limitations before production

The supplied event contracts are not all identical. This implementation uses explicit top-level projection fields and event facts but intentionally does not invent fields absent from contracts. Fine-grained department/project authorization must be aligned with the platform's JWT scope claims and policy before production. Period summaries use UTC calendar boundaries; financial-year-specific boundaries require the platform's approved financial-year rule. Configure Kafka error handling/dead-letter topics and a tested transactional or replay strategy according to the root platform's Kafka standards.


## Phase 2 delivery notes

- MongoDB indexes are created at startup using `createIndex`.
- Environment-specific endpoints and credentials should be supplied through environment variables, not committed secrets.
- Run the root Gradle test/build commands listed in `docs/reporting-service-roadmap.md` after extracting this ZIP.
