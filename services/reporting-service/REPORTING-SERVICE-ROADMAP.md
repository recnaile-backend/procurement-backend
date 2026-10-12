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
| 12 | Indexes and performance | Implemented; local verification pending |
| 13 | Rebuild and recovery | Implemented in this package; verification pending |
| 14 | Tests, documentation and observability | Remaining |
| 15 | Platform integration and release | Remaining |

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

All requests require a valid JWT. The snapshot capture route requires MD or REPORTING. Department/project filters are constrained by token scope; a multi-project caller must choose one allowed project. Confirm the role claims and HS256 secret configuration match the Auth Service before release.

## Phase 13 — Rebuild and recovery

- Added `GET /api/v1/reporting/admin/recovery-status` for event-fact, projection, and snapshot counts.
- Added `POST /api/v1/reporting/admin/rebuild-projections?confirm=true`. The explicit confirmation protects against accidental rebuilds; the route inherits the existing MD/REPORTING admin authorization.
- The rebuild stops the Reporting Kafka listener, deletes only derived projections, and replays retained event facts in occurrence/ingestion order in bounded batches. It then restarts the listener if it was running.
- Event facts and historical snapshots are preserved. If a rebuild is interrupted, it can be safely rerun from the immutable fact collection.
- Recovery can reconstruct only values retained in `reporting_event_facts`; fields that were never stored in facts (for example some low-stock alert details) cannot be recovered from this service alone. Reconcile those against authoritative source events if full historical fidelity is required.

### Phase 13 recovery runbook

1. Check `GET /api/v1/reporting/admin/recovery-status`.
2. Take/verify a MongoDB backup before a production rebuild.
3. Run `POST /api/v1/reporting/admin/rebuild-projections?confirm=true` with an MD/REPORTING JWT during a maintenance window.
4. Confirm `factsReplayed` matches `eventFactsAtStart`, then check dashboard/report endpoints and service health.
5. If the operation fails part-way, inspect logs and rerun the rebuild; do not delete `reporting_event_facts` or `reporting_snapshots`.
