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
