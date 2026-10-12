# Reporting Service — Phase 8 Delivery

## Scope

Phase 3 strengthens the typed reporting read models and stable document identity while preserving the Phase 1–2 service foundation and APIs.

- `ReportingFact` validates required event identity, producer, entity identity and timestamps.
- `ReportingProjection` validates required identity and timestamps and requires `id == entityType + ":" + entityId`.
- `ReportingProjection.keyFor(...)` centralizes the composite key rule used by the event processor.
- Added unit tests for required identity, deterministic keys, identity mismatch, and a valid projection.
- Existing MongoDB document names, indexes, repository APIs, and endpoint contracts are retained.

## Validation status

Archive structure and source-level changes were inspected before packaging. This packaging environment does not have the user's root Gradle wrapper/dependency cache, so a fresh Gradle test/build could not be executed here. No ZIP can be honestly guaranteed error-free without running the exact archive against the root repository. Run the verification commands in the roadmap before deployment.

## Compatibility notes

- Projection IDs remain `ENTITY_TYPE:entityId` (for example, `INDENT:IND-42`).
- A legacy projection whose stored `id` does not match its `entityType` and `entityId` will fail validation if reconstructed through this record constructor; inspect existing MongoDB data before deploying if such records may exist.
- Reporting remains a read model and must not query operational service databases directly.


## Phase 4 — Event envelope and contract validation

- Added `ReportingEventContractValidator` for envelope shape, event version, ISO-8601 timestamp, supported event type, and required payload-field checks.
- Integrated validation before event mapping/persistence in `ReportingEventProcessor`.
- Added focused tests for valid events, missing envelope fields, missing payload fields, unsupported types, and invalid timestamps.
- Validation is contract-aligned required-field validation, not a full JSON Schema implementation; full enum/type/additional-property enforcement remains future hardening.
- Run the root Gradle test and build commands before deploying this ZIP.

## Phase 6 — Indent and purchase-order projections

- Purchase-order events (`PO_CREATED`, `PO_ISSUED`, `PO_CLOSED`) map to one stable `PURCHASE_ORDER:{poId}` read model.
- `PO_CREATED` links the projection to its source indent and vendor, and aggregates line quantities and extended line values from the event's `items` array.
- Later PO status events update status without dropping previously projected vendor, indent, quantity, or estimated-value data.
- Added tests for multi-line order aggregation and preservation of totals when a PO is issued.
- The event contract remains the source of truth: quantity and unit price must be numeric; no additional non-negative business constraint is introduced beyond the supplied schema.
- Verification status: source and ZIP integrity checked here; run the root Gradle test/build commands before release because this environment does not contain the repository Gradle wrapper.


## Phase 7 — Inward, inventory, outward, return and asset projections

- `GOODS_RECEIVED` projects accepted quantity as the inward receipt quantity and preserves ordered, accepted, rejected, GRN and PO references.
- `REJECTION_FLAGGED` updates the same GRN projection and preserves previously projected receipt details instead of creating a second inward entity.
- `LOW_STOCK_ALERT` creates/updates a `STOCK:{itemId}` projection with `LOW_STOCK`, reported available quantity and reorder threshold. This is the latest alert snapshot, not a complete authoritative stock ledger.
- `OUTWARD_ISSUED` projects issued quantity, item, department and recipient.
- `RETURN_RECORDED` projects return quantity, condition, item, actor and optional asset reference.
- `ASSET_STATUS_CHANGED` projects asset lifecycle status; `ASSET_CLOSURE_REQUESTED` updates the same asset projection with `CLOSURE_REQUESTED`; `ASSET_CREATION_REQUESTED` is linked to its originating outward request.
- Added focused regression tests for inward receipts, rejection updates, stock alerts, outward issues, returns, asset status, asset closure and asset creation linkage.
- Verification status: source changes and ZIP structure are checked; this environment does not have the repository's Gradle wrapper/dependency cache, so run the Gradle test/build commands in the root repository before deployment.


## Phase 8 — KPI/dashboard aggregation

- Expanded dashboard aggregation across indent status and approval aging, purchase-order lifecycle, inward accepted/rejected quantities, low-stock alert observations, outward activity, returns, assets and SLA breaches.
- Added optional department/project filtering to dashboard KPIs. The controller resolves requested filters through `ReportingScopePolicy`, so non-global callers cannot widen their JWT-defined reporting scope.
- Included the applied filters and freshness metadata in the dashboard response.
- KPI queries use only reporting-service projections/facts; no operational service database calls were added.
- Low-stock values are explicitly limited to latest observed alert projections and must not be treated as a complete stock ledger.

## Verification status

Archive integrity and source edits were checked while packaging. This delivery environment does not contain the root Gradle wrapper/build files or a Gradle executable, so Gradle tests/build and Docker health could not be run here. Run the commands below from the full repository root before deployment.


## Phase 8 — KPI and dashboard services

- Added scoped KPI aggregation for indent lifecycle and aging, purchase-order lifecycle and values, inward quantities, low-stock alert observations, outward activity, returns, assets, and SLA breaches.
- Applied the same JWT-resolved department/project scope to dashboard summary and freshness queries.
- Added dashboard generation time, applied filters, scoped event count and freshness metadata.
- Added unit tests for dashboard output/scope and reporting-scope policy.
- Phase status: implementation packaged; Gradle and representative fixture verification must pass in the full repository before this phase is marked complete.
