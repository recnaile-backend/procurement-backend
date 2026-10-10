# Audit Service — Phase 02: MongoDB Persistence

## Scope delivered
- Adds `AuditEventDocument` mapped to MongoDB collection `audit_events`.
- Uses the platform event UUID as Mongo `_id`, providing a unique identity at the storage layer.
- Maps the existing Phase 01 envelope and payload fields without inventing new event fields.
- Adds `AuditEventMongoRepository` query methods for entity history, actor history, correlation/trace investigation, and service/action filtering.
- Adds compound indexes for entity history (`entityType`, `entityId`, descending `timestamp`), actor history, service/action/time, and sparse correlation/trace IDs.
- Adds `AuditEventPersistenceService.append`; it uses Mongo `insert`, not `save`, to prevent replacing an existing audit record with the same ID. Duplicate IDs fail and are not silently overwritten.
- Adds mapping, index metadata, and persistence-service unit tests.

## Important boundary
This phase adds the persistence building blocks only. It does not yet connect Kafka ingestion, implement consumer idempotency/retry/DLT, expose search APIs, or establish JWT/RBAC. Those remain later phases. The existing `RecordDocument`/`RecordRepository` and controller are retained for compatibility but are not used by the new append-only audit persistence path.

## Verification
Run tests and build from the repository root so the existing Gradle multi-project settings and dependency versions are used. These commands must be executed in the user's full repository; this package alone does not contain the root Gradle wrapper/settings.
