# Reporting Service — Implementation Status through Phase 11

## Roadmap status

| Phase | Area | Status |
|---:|---|---|
| 1 | Repository and contract audit | Complete |
| 2 | Service foundation | Complete — previously verified |
| 3 | Typed reporting read models | Complete — previously verified |
| 4 | Event contract validation | Complete — previously verified |
| 5 | Reliable Kafka consumers | Baseline present; integration verification pending |
| 6 | Indent and purchase-order projections | Complete — previously verified |
| 7 | Inward, inventory, outward, return and asset projections | Packaged; verify against full repository |
| 8 | KPI and dashboard services | User-confirmed complete |
| 9 | Time-period reports and snapshots | Implemented in this package; verification pending |
| 10 | Reporting APIs | Implemented in this package; verification pending |
| 11 | Security and scope enforcement | Implemented in this package; verification pending |
| 12 | Indexes and performance | Implemented in this package; verification pending |
| 13 | Rebuild and recovery | Remaining |
| 14 | Tests, documentation and observability | Remaining |
| 15 | Platform integration and release | Remaining |

## Phase 9 — Time-period reports and snapshots

- Supports daily, weekly, monthly, quarterly, half-yearly and yearly UTC buckets.
- Period summaries aggregate retained event facts with resolved department/project filters.
- Validates range ordering and limits date ranges to ten years.
- Captures point-in-time KPI values and freshness metadata in reporting snapshots.
- Snapshot IDs distinguish date and scope while preserving the legacy global snapshot ID.

## Phase 10 — Reporting APIs

- Period-summary, snapshots, projections and events endpoints support filtering, pagination and endpoint-specific allowlisted sorting.
- Page size is capped at 200. Invalid sort fields/directions and reversed date ranges return HTTP 400 Problem Details.
- Added an admin snapshot capture endpoint for MD/REPORTING roles.

## Phase 11 — Security and scope enforcement

- All report/dashboard endpoints resolve JWT scope before querying.
- Non-global callers without department/project scope claims are denied.
- Conflicting department/project requests are denied; multi-project claims require a selected allowed project.
- Snapshot reads are scoped to an exact department/project pair and do not expose global snapshots to scoped callers.
- Role mapping and JWT secret configuration remain dependent on alignment with the platform Auth Service and production security standard.

## Verification

The archive is packaged from the user's Phase 8 test-compile-fix ZIP. Source and archive structure are checked during packaging, but this environment does not contain the full root repository Gradle wrapper/dependency context or the user's MongoDB/Kafka services. Run the commands in `REPORTING-SERVICE-ROADMAP.md` from the repository root. Do not mark Phases 9–11 verified until Gradle tests/build and representative API/security integration checks pass.

## Important semantics

- Reporting is eventually consistent.
- Period reports summarize retained immutable event facts; dashboards summarize current denormalized projections.
- Approval aging uses projection `updatedAt` until an authoritative approval-start timestamp is available.
- Low-stock metrics describe observed low-stock alerts, not a complete stock ledger.
- Reporting must not query operational service databases directly.


## Phase 12 — Indexes and performance

- Centralized index creation in `MongoIndexConfiguration`; Spring Data automatic index creation is disabled so indexes are managed consistently by the service startup runner.
- Added named indexes for event-type/time queries, event history, ingestion freshness, scoped time ranges, dashboard SLA counts, projection uniqueness, scoped projection listing/counting, aging queries, and snapshot scope/date lookups.
- Kept indexes aligned to the current read paths and documented a verification procedure using MongoDB `explain("executionStats")`.
- Startup index creation is idempotent by stable index names. The known legacy `snapshotDate_1` index is removed to support the date-plus-scope uniqueness key.
- Unique index creation can fail if existing data contains duplicate projection keys or duplicate snapshot date/scope combinations; preflight checks are documented before production rollout.

## Phase 12 verification status

The ZIP has been structurally checked, but Gradle tests and MongoDB query-plan checks must be run in the full repository environment. Do not mark Phase 12 verified until the service builds, tests pass, startup indexes are confirmed in MongoDB, and representative scoped queries use the intended indexes.
