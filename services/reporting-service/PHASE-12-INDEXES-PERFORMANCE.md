# Phase 12 — Indexes and Performance

## Goals

Keep reporting reads efficient without introducing operational-service database queries. Indexes target the filters and sort orders used by the reporting dashboard, period summaries, event/projection list APIs, freshness lookup, and scoped snapshots.

## Managed indexes

### `reporting_event_facts`

| Index | Keys | Primary query path |
|---|---|---|
| `fact_type_occurred` | `eventType ASC, occurredAt DESC` | Event-type and time filtering; SLA/event metrics |
| `fact_entity_occurred` | `entityType ASC, entityId ASC, occurredAt DESC` | Entity event history |
| `fact_ingested_at` | `ingestedAt DESC` | Latest-ingestion freshness |
| `fact_occurred_at` | `occurredAt DESC` | Global period ranges and event listing |
| `fact_scope_occurred` | `departmentName ASC, projectId ASC, occurredAt DESC` | Scoped period summaries |
| `fact_type_scope_occurred` | `eventType ASC, departmentName ASC, projectId ASC, occurredAt DESC` | Scoped event-type counts |

### `reporting_projections`

| Index | Keys | Primary query path |
|---|---|---|
| `projection_entity_unique` | `entityType ASC, entityId ASC` (unique) | Stable projection identity/idempotent upsert |
| `projection_type_status_updated` | `entityType ASC, status ASC, updatedAt DESC` | Status counts and approval aging |
| `projection_department_updated` | `departmentName ASC, updatedAt DESC` | Department-filtered recent projections |
| `projection_type_scope_updated` | `entityType ASC, departmentName ASC, projectId ASC, updatedAt DESC` | Scoped projection listing and sorting |
| `projection_type_status_scope_updated` | `entityType ASC, status ASC, departmentName ASC, projectId ASC, updatedAt DESC` | Scoped status KPI queries |

### `reporting_snapshots`

| Index | Keys | Primary query path |
|---|---|---|
| `snapshot_date_scope_unique` | `snapshotDate ASC, departmentName ASC, projectId ASC` (unique) | One snapshot per date and exact scope |
| `snapshot_scope_date` | `departmentName ASC, projectId ASC, snapshotDate DESC` | Scoped newest-first snapshot history |

## Safe rollout notes

1. Back up the reporting database before deploying index changes in a persistent environment.
2. Before the unique projection index is created, confirm there is no more than one document for each `(entityType, entityId)` pair.
3. Before the unique snapshot index is created, confirm there is no more than one document for each `(snapshotDate, departmentName, projectId)` tuple. Treat `null` and missing scope fields consistently; normalize legacy global snapshots before rollout if necessary.
4. The startup runner removes only the known legacy `snapshotDate_1` index. Review existing index names in staging before production deployment; do not manually delete unrelated indexes.
5. Index builds consume storage and can increase event-ingestion write cost. Validate index size and write latency with representative data before scaling up the index set.

## Verify from the repository root

```powershell
.\gradlew :services:reporting-service:clean :services:reporting-service:test
.\gradlew :services:reporting-service:build
docker compose build reporting-service
docker compose up -d reporting-service
docker compose logs --tail=200 reporting-service
```

If using `compose-debug.yml`, add `-f compose-debug.yml` to each Docker Compose command.

## Verify index definitions in MongoDB

Run in `mongosh` against the reporting database after the service starts:

```javascript
db.reporting_event_facts.getIndexes()
db.reporting_projections.getIndexes()
db.reporting_snapshots.getIndexes()

// Check duplicates before a unique index rollout.
db.reporting_projections.aggregate([
  { $group: { _id: { entityType: '$entityType', entityId: '$entityId' }, count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } },
  { $limit: 20 }
])

db.reporting_snapshots.aggregate([
  { $group: { _id: { snapshotDate: '$snapshotDate', departmentName: '$departmentName', projectId: '$projectId' }, count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } },
  { $limit: 20 }
])
```

## Verify query plans

Use representative values from the staging dataset. The planner can select a different index depending on data distribution; compare `totalDocsExamined`, `totalKeysExamined`, and execution time rather than requiring one fixed plan shape.

```javascript
db.reporting_event_facts.find({
  departmentName: 'DEPT-7', projectId: 'PROJECT-9',
  occurredAt: { $gte: ISODate('2026-10-01T00:00:00Z'), $lt: ISODate('2026-11-01T00:00:00Z') }
}).sort({ occurredAt: 1 }).explain('executionStats')

db.reporting_projections.find({
  entityType: 'INDENT', status: 'PENDING_APPROVAL',
  departmentName: 'DEPT-7', projectId: 'PROJECT-9',
  updatedAt: { $lt: ISODate('2026-10-11T00:00:00Z') }
}).explain('executionStats')

db.reporting_snapshots.find({ departmentName: 'DEPT-7', projectId: 'PROJECT-9' })
  .sort({ snapshotDate: -1 }).limit(25).explain('executionStats')
```

## Known performance boundary

The period-summary implementation currently materializes matching facts to build period buckets in application memory. The indexes reduce scan work, but very large date ranges may still require a later server-side aggregation or bounded asynchronous report job. Do not silently truncate results. This is a measured follow-up, not a change in report semantics for this phase.


### Numeric API values

Reporting quantities, estimated values, unit prices, stock thresholds, and SLA hour values are exposed as whole-number integers. Incoming numeric values containing a fractional component are rounded to the nearest integer during event ingestion.
