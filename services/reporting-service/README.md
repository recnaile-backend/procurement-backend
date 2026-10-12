# Reporting Service

Read-optimized reporting boundary for the RECNAILE Procurement Platform. It consumes procurement domain events from Kafka and maintains its own MongoDB projections in `reporting` (or `MONGODB_DATABASE`). It does not query operational service databases.


## Swagger UI and API error documentation

- Swagger UI: `http://localhost:8092/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8092/v3/api-docs`
- OpenAPI YAML: `http://localhost:8092/v3/api-docs.yaml`
- Full endpoint, JWT, status-code, and error-response reference: [`docs/API-AND-ERROR-DOCUMENTATION.md`](docs/API-AND-ERROR-DOCUMENTATION.md)

Errors use a consistent RFC 7807-style `ApiProblem` body with `status`, `code`, `detail`, `instance`, and `correlationId`. Swagger UI includes the JWT Bearer **Authorize** control.

## APIs

- `GET /api/v1/reporting/dashboard/summary?departmentId=...&projectId=...` — operational KPI counts, aging, procurement/inventory activity, and applied scope filters. Department/project values are constrained by JWT reporting scope claims.
- `GET /api/v1/reporting/dashboard/freshness` — latest observed event/update time and event count.
- `GET /api/v1/reporting/reports/period-summary?period=DAILY|WEEKLY|MONTHLY|QUARTERLY|HALF_YEARLY|YEARLY&from=YYYY-MM-DD&to=YYYY-MM-DD` — event counts and quantity totals for UTC date boundaries.
- `GET /api/v1/reporting/reports/projections?page=0&size=25&sort=updatedAt,desc` — pageable projection listing with entityType/status/departmentId/projectId filters.
- `GET /api/v1/reporting/reports/events` — pageable retained event facts; optional eventType/from/to/departmentId/projectId filters.
- `GET /api/v1/reporting/reports/snapshots?departmentId=...&projectId=...&page=0&size=25&sort=snapshotDate,desc` — pageable historical KPI snapshots for exactly the caller's resolved scope.
- `POST /api/v1/reporting/admin/snapshots?date=YYYY-MM-DD&departmentId=...&projectId=...` — capture a point-in-time snapshot; MD role only. Omitting department/project captures a platform-wide snapshot for global roles.

All API endpoints require a JWT. The service validates HS256 tokens using `JWT_SECRET_KEY`, matching the Auth Service and existing resource-server configuration in the supplied repository. The secret must be at least 32 UTF-8 bytes. Confirm the exact issuer/validator policy with the platform security standard before release. Role claims `roles` or `authorities` are mapped to `ROLE_*`. Dashboard and detailed-report role requirements are declared in `config/SecurityConfig.java`. Verify claim shape against the platform Auth Service before release.

## Event ingestion

A configurable Kafka topic regex (`REPORTING_TOPIC_PATTERN`) selects platform event topics. Messages must use the platform envelope (`eventId`, `eventType`, `eventVersion`, `occurredAt`, `producer`, `payload`). Immutable facts are keyed by `eventId`, and current entity projections are upserted by `entityType:entityId`; replaying a duplicate event therefore does not create duplicate facts or additive counts. Unsupported event types are retained as `UNMAPPED` facts and do not create current projections.

## Consistency and rebuild

Reports are eventually consistent. Freshness endpoints make the latest observed event and ingestion time visible. Use `GET /api/v1/reporting/admin/recovery-status` to inspect retained facts, derived projections and snapshots, then `POST /api/v1/reporting/admin/rebuild-projections?confirm=true` to rebuild derived projections from retained facts. This operation pauses the Kafka listener, clears only `reporting_projections`, replays retained facts, preserves event facts and historical snapshots, and restores the listener state. Back up MongoDB and use an approved maintenance window before a production rebuild. Events older than Kafka retention or fields absent from retained facts require an approved source-data export/replay mechanism. Never delete `reporting_event_facts` as part of this recovery endpoint's procedure.

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


## Phase 3 — typed read-model identity

Reporting facts validate required event identity and timestamps. Reporting projections use a stable composite key generated by `ReportingProjection.keyFor(entityType, entityId)` and reject inconsistent identity tuples. Focused model tests are under `src/test/java/com/recnaile/reporting/model`.


## Event contract validation (Phase 4)

Kafka messages are validated before they are written to reporting facts or projections. The validator checks the JSON envelope (`eventId`, `eventType`, `eventVersion`, `occurredAt`, `producer`, and object-valued `payload`), rejects unknown event types, and checks required payload fields aligned with `contracts/events/*.schema.json`. The contract registry is maintained in `ReportingEventContractValidator`; update it alongside event contract changes. Invalid events raise an exception so the configured Kafka error handler can retry and route persistent failures to the topic dead-letter topic.

The current implementation validates required-field presence and core envelope semantics; it is not a complete JSON Schema Draft-07 engine and does not enforce every property type, enum, or `additionalProperties` rule in every producer schema.

## Phase 6: indent and purchase-order projections

The reporting processor projects indent lifecycle events and purchase-order lifecycle events into stable, denormalized records. `PO_CREATED` records the PO-to-indent and vendor relationships and calculates aggregate ordered quantity and estimated line value from each item row (`quantity * unitPrice`). `PO_ISSUED` and `PO_CLOSED` update lifecycle status while retaining previously known values when the event does not repeat those fields. Reporting remains read-only and does not query the purchase-order service's operational database.

Validate the delivery from the repository root:

```powershell
.\gradlew :services:reporting-service:clean :services:reporting-service:test
.\gradlew :services:reporting-service:build
docker compose build reporting-service
docker compose up -d reporting-service
Invoke-RestMethod http://localhost:8092/actuator/health
```



## Phase 7 lifecycle projections

The reporting projection processor handles GRN receipt/rejection events, low-stock alert snapshots, outward issue events, return events, and asset lifecycle events. `LOW_STOCK_ALERT` is only the latest quantity reported by that alert; it is not a substitute for an authoritative inventory ledger. Run the repository-level Gradle test and build commands before deployment.


## Phase 8 — KPI and dashboard aggregation

The dashboard summary aggregates KPI counts and quantities from `reporting_projections` and `reporting_event_facts` only. It supports optional department/project filters and applies the caller's JWT reporting scope before querying. KPIs include pending/aging indents, PO lifecycle counts, inward accepted/rejected quantities, latest observed low-stock alert quantities, outward issues, returns, assets and SLA breaches. The response includes applied filters and report freshness.

**Interpretation note:** low-stock values summarize the latest low-stock alert projections; they are not a complete authoritative inventory ledger. Reporting is eventually consistent.


## Phase 8 dashboard aggregation

The dashboard response contains `generatedAt`, the applied `filters`, a `freshness` object and operational KPI fields. Optional department/project filters are constrained by JWT claims through `ReportingScopePolicy`; a caller cannot widen a department or project scope granted by the token. Aggregations execute only against this service's own `reporting_projections` and `reporting_event_facts` collections.

The approval-aging metrics use the projection's `updatedAt` as the current aging reference because the projection currently does not expose a dedicated immutable approval-start timestamp. Treat these metrics as projection-update aging until an authoritative approval timestamp is included in the reporting read model. Low-stock KPIs describe items observed in low-stock alert events only; they are not a complete inventory balance. All dashboard figures are eventually consistent.


## Phase 9 — Time-period reports and historical snapshots

- Period summaries support `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `HALF_YEARLY`, and `YEARLY` UTC buckets.
- Optional date bounds are validated; `to` cannot precede `from`, and a report range is limited to ten years to avoid accidental unbounded scans.
- Period counts and quantity totals are derived from retained `reporting_event_facts`, filtered by the resolved department/project scope.
- Daily snapshots retain point-in-time KPI values and freshness timestamps rather than recomputing past dashboard values from mutable current projections.
- Snapshot identity includes date and scope. Global snapshots preserve the legacy date-based ID for compatibility; scoped snapshots use a deterministic date/department/project key.

## Phase 10 — Reporting API contracts and query controls

- Dashboard, freshness, period summary, projection, event-fact and snapshot endpoints expose stable JSON responses.
- List endpoints support page/size/sort controls; page size is limited to 200 and sort fields/directions are allowlisted per endpoint.
- Event date ranges and report date ranges are validated. Invalid filters and sort fields return HTTP 400 Problem Details.
- `POST /api/v1/reporting/admin/snapshots` is restricted to the MD role.
- The reporting service only reads its own MongoDB database. It does not call operational services or their databases.

## Phase 11 — Security and scope enforcement

- Every dashboard and report query resolves its requested department/project filters through `ReportingScopePolicy` before accessing data.
- Global reporting roles (`MD`, `REPORTING`, `PROCUREMENT`) can query platform-wide or explicitly filtered scope. Non-global roles must carry a department or project scope claim.
- A requested department/project that conflicts with token claims is rejected. A multi-project token must select one allowed project rather than accidentally querying an unrestricted set.
- Snapshot list queries match the exact resolved scope; a department/project user cannot read platform-wide snapshots.
- The API gateway and identity provider must issue trusted, validated scope claims. Review the role matrix and JWT signing/issuer configuration against the platform security standard before production.

## Phase 9–11 operational notes

The report boundary is eventually consistent. Period summaries describe retained event facts, while dashboards describe the current projections; these are different views and should not be expected to reconcile instantaneously. A scheduled daily snapshot captures the platform-wide scope. Scope-specific snapshots can be captured by an MD-authorized operator. Snapshot uniqueness is date + department + project; startup index setup removes the old single-field `snapshotDate_1` index when present and installs the scope-aware compound index. Back up the reporting database before first deployment of this schema change.


## Indexes and query performance

Reporting indexes are explicitly managed by `MongoIndexConfiguration`. Automatic annotation-driven index creation is disabled to avoid duplicate/conflicting definitions. See `PHASE-12-INDEXES-PERFORMANCE.md` for index coverage, safe rollout checks, and MongoDB execution-plan commands.


## Phase 13 — projection rebuild and recovery

Users with the `MD` role can inspect recovery counts at `GET /api/v1/reporting/admin/recovery-status`. To rebuild derived projections from retained event facts, call `POST /api/v1/reporting/admin/rebuild-projections?confirm=true`. The endpoint stops the Kafka listener while rebuilding, preserves event facts and historical snapshots, and restarts the listener if it was running. Back up MongoDB first and validate dashboards after a rebuild. Only data retained in the fact model can be reconstructed; omitted historical payload fields require replay from the authoritative source.


## Phase 14 — tests, documentation and observability

- HTTP requests propagate a validated `X-Correlation-ID` response header and place it in logging MDC; invalid or missing values are replaced with a generated UUID.
- Log level output includes service, trace ID, span ID and correlation ID fields.
- Actuator exposes health, metrics and Prometheus scrape endpoints. Reporting collection gauges expose fact, projection and snapshot counts. Kafka ingestion metrics track messages received, processing failures and processing duration.
- Focused correlation-ID tests cover safe propagation, unsafe-header replacement and MDC cleanup.
- See `PHASE-14-TESTS-DOCUMENTATION-OBSERVABILITY.md` for verification commands, metrics and operational guidance.


## Phase 15 — Platform integration and release

- Liveness: `GET /livez`; readiness: `GET /readyz` (includes MongoDB health). Health details remain hidden from unauthenticated callers.
- Release metadata: `GET /actuator/info`, configured with `REPORTING_SERVICE_VERSION` and `REPORTING_RELEASE_TRACK`.
- Graceful shutdown is enabled with a 20-second shutdown phase timeout; Docker health checks use `/livez`.
- See `PHASE-15-PLATFORM-INTEGRATION-RELEASE.md` and `scripts/verify-reporting-release.ps1` for the release checklist.
