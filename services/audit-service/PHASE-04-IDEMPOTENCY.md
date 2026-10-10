# Phase 04 — Consumer idempotency and duplicate handling

## Goal
Make at-least-once Kafka delivery safe without dropping conflicting data or updating existing audit records.

## Design
1. The event UUID is persisted as MongoDB `_id`, providing a durable uniqueness constraint.
2. New events use Mongo repository `insert()` only.
3. If insert reports a duplicate key, the service loads the stored document by the same UUID.
4. If the persisted document exactly matches the incoming mapped document, this is an idempotent replay. The consumer returns normally and Phase 3's listener can acknowledge the Kafka record.
5. If the ID exists with different content, `AuditEventIdentityConflictException` is thrown. It is not acknowledged as a success and remains subject to Phase 3 retry/DLT handling.
6. If the duplicate exception occurs but no document can be found, the original persistence exception is propagated.

## Failure cases covered
- Duplicate delivery after Mongo insert but before Kafka offset acknowledgement.
- Exact duplicate delivery after restart/rebalance.
- Event ID collision with changed payload.
- Null input rejected before repository access.
- Persistence remains append-only; existing records are never overwritten.

## Verification
From the repository root, run:

```powershell
.\gradlew.bat :services:audit-service:test
.\gradlew.bat :services:audit-service:build
```

The full repository's Gradle wrapper and dependency cache are required to confirm compilation and tests.
