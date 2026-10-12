# Phase 13 — Rebuild and Recovery

## Included

- Recovery status endpoint: `GET /api/v1/reporting/admin/recovery-status`.
- Projection rebuild endpoint: `POST /api/v1/reporting/admin/rebuild-projections?confirm=true`.
- The rebuild requires explicit confirmation and is restricted to the MD role.
- The Kafka listener is stopped while replay runs and restarted afterward if it was previously running.
- Only derived projections are deleted. Immutable event facts and historical snapshots are retained.
- Facts are replayed in occurrence, ingestion, and event-ID order in batches of 500.
- A unit test covers replay invocation and preservation of facts/snapshots.

## Runbook

1. Verify a current MongoDB backup before a production rebuild.
2. Query `GET /api/v1/reporting/admin/recovery-status` using an MD JWT.
3. Run `POST /api/v1/reporting/admin/rebuild-projections?confirm=true`.
4. Confirm `factsReplayed` equals `eventFactsAtStart` in the response.
5. Verify service health, dashboard totals, and representative scoped reports.
6. If replay fails part-way, inspect service logs and rerun the rebuild. Do not delete the event-fact or snapshot collections.

## Recovery boundary

Only values persisted in `reporting_event_facts` can be reconstructed. Fields that were never persisted in the fact model (including some low-stock alert details) cannot be recovered from this service's facts alone. Use authoritative producer events/backups if full reconstruction of those fields is required. Reporting remains eventually consistent.

## Verification commands

Run from the `procurement-backend` repository root:

```powershell
.\gradlew :services:reporting-service:clean :services:reporting-service:test
.\gradlew :services:reporting-service:build
docker compose build reporting-service
docker compose up -d reporting-service
docker compose ps reporting-service
Invoke-RestMethod http://localhost:8092/actuator/health
docker compose logs --tail=200 reporting-service
```


### Numeric API values

Reporting quantities, estimated values, unit prices, stock thresholds, and SLA hour values are exposed as whole-number integers. Incoming numeric values containing a fractional component are rounded to the nearest integer during event ingestion.
