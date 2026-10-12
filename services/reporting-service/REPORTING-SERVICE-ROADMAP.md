# Reporting Service Roadmap and Verification Commands

## Roadmap

| Phase | Area | Status |
|---:|---|---|
| 1 | Repository and contract audit | Complete |
| 2 | Service foundation | Complete |
| 3 | Typed reporting read models | Complete |
| 4 | Event contract validation | Complete |
| 5 | Reliable Kafka consumers | Integration verification pending |
| 6 | Indent and purchase-order projections | Complete |
| 7 | Inward, inventory, outward, return and asset projections | Packaged; verify ZIP |
| 8 | KPI and dashboard services | User-confirmed complete |
| 9 | Time-period reports and snapshots | Implemented; verify |
| 10 | Reporting APIs | Implemented; verify |
| 11 | Security and scope enforcement | Implemented; verify |
| 12 | Indexes and performance | Complete; user-confirmed build/container healthy |
| 13 | Rebuild and recovery | Complete; user-confirmed build/container healthy |
| 14 | Tests, documentation and observability | Complete; user-confirmed build/container healthy |
| 15 | Platform integration and release | Implemented in this package; final platform verification required |

## Verify from the procurement-backend repository root

```powershell
.\gradlew :services:reporting-service:clean :services:reporting-service:test
.\gradlew :services:reporting-service:build
docker compose build reporting-service
docker compose up -d reporting-service
docker compose ps reporting-service
Invoke-RestMethod http://localhost:8092/actuator/health
docker compose logs --tail=200 reporting-service
```

If using `compose-debug.yml`, add `-f compose-debug.yml` to every Docker Compose command.

## API examples

```text
GET  /api/v1/reporting/reports/period-summary?period=MONTHLY&from=2026-01-01&to=2026-10-12&departmentId=DEPT-7
GET  /api/v1/reporting/reports/snapshots?page=0&size=25&sort=snapshotDate,desc&departmentId=DEPT-7
POST /api/v1/reporting/admin/snapshots?date=2026-10-12&departmentId=DEPT-7&projectId=PROJECT-9
```

All requests require a valid JWT. The snapshot capture route requires the MD role. Department/project filters are constrained by token scope; a multi-project caller must choose one allowed project. Confirm the role claims and HS256 secret configuration match the Auth Service before release.

## Phase 13 — Rebuild and recovery

- Added `GET /api/v1/reporting/admin/recovery-status` for event-fact, projection, and snapshot counts.
- Added `POST /api/v1/reporting/admin/rebuild-projections?confirm=true`. The explicit confirmation protects against accidental rebuilds; the route is restricted to the MD role.
- The rebuild stops the Reporting Kafka listener, deletes only derived projections, and replays retained event facts in occurrence/ingestion order in bounded batches. It then restarts the listener if it was running.
- Event facts and historical snapshots are preserved. If a rebuild is interrupted, it can be safely rerun from the immutable fact collection.
- Recovery can reconstruct only values retained in `reporting_event_facts`; fields that were never stored in facts (for example some low-stock alert details) cannot be recovered from this service alone. Reconcile those against authoritative source events if full historical fidelity is required.

### Phase 13 recovery runbook

1. Check `GET /api/v1/reporting/admin/recovery-status`.
2. Take/verify a MongoDB backup before a production rebuild.
3. Run `POST /api/v1/reporting/admin/rebuild-projections?confirm=true` with an MD JWT during a maintenance window.
4. Confirm `factsReplayed` matches `eventFactsAtStart`, then check dashboard/report endpoints and service health.
5. If the operation fails part-way, inspect logs and rerun the rebuild; do not delete `reporting_event_facts` or `reporting_snapshots`.


## Phase 14 — Tests, documentation and observability

- Request correlation IDs are returned as `X-Correlation-ID`, added to MDC during request handling and removed afterward. Unsafe values are replaced.
- Prometheus metrics include event counters, Kafka receive/failure counters and consume timer, plus reporting fact/projection/snapshot gauges.
- Logging pattern includes trace/span/correlation IDs; OTLP exporter and tracing bridge are retained.
- Focused filter tests and an operational metrics runbook are included.
- Verify Gradle tests/build, `/actuator/health`, `/actuator/prometheus`, and a representative API request with a correlation ID before marking complete.


## Phase 15 — Platform integration and release

- Added `/livez` and `/readyz` probe paths on the application port. Liveness checks process state; readiness checks process readiness and MongoDB connectivity. Health details are not exposed publicly.
- Docker health checks use `/livez`; Spring graceful shutdown allows active requests and lifecycle components up to 20 seconds to stop.
- `/actuator/info` reports service name, description, release version (`REPORTING_SERVICE_VERSION`) and release track (`REPORTING_RELEASE_TRACK`).
- Added a release verification script and a platform release checklist. This package does not include the root platform's Compose, gateway, Prometheus, or Grafana files; validate those against the actual repository before release.
- Phase 15 is package-complete, but the final release remains subject to the root Gradle test/build, Docker health, authenticated API smoke test, Kafka event ingestion, and observability checks below.

### Release verification

From the repository root, run:

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

`/livez` and `/readyz` should return HTTP 200; readiness should not pass while MongoDB is unavailable. The dashboard/report APIs require a valid JWT, so run at least one authenticated smoke test using a test token issued under the platform's current Auth Service policy. Publish or replay one representative event in a non-production environment and verify a corresponding reporting fact/projection and the ingestion metrics.
