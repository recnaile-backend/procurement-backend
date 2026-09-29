# Phase 08 Verification

## Idempotency & Optimistic Concurrency

- `Idempotency-Key` is optional.
- When omitted or blank, issue processing proceeds without idempotency lookup.
- When supplied, the key is normalized and limited to 128 characters.
- A repeated key for the same outward returns the original issue transaction.
- Reusing a key for a different outward returns `OUTWARD_IDEMPOTENCY_KEY_REUSED`.
- Duplicate outward issue remains protected by the outward-number uniqueness rule.
- Optimistic locking conflicts map to `OUTWARD_CONCURRENT_MODIFICATION`.
- Issue transaction processing does not directly modify Inventory Service/database.
- The non-approved-request test verifies that no issue transaction is saved while allowing the expected repository lookup performed by the service.

## Verification status

Source-level package and Java-structure checks completed after the test correction. Run the repository Gradle test/build commands in the target procurement-backend checkout to perform the authoritative integration/build verification.
