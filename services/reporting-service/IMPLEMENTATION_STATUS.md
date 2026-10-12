# Reporting Service — Phase 3 Delivery

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

