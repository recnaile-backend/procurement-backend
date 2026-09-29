# Phase 09 Verification

## Transactional Outbox

- An `outbox_events` MongoDB collection is introduced for reliable asynchronous event delivery.
- `OutwardIssueService` writes the issue transaction, changes the outward request to `ISSUED`, and enqueues the corresponding `OUTWARD_ISSUED` outbox event inside the same Spring transaction boundary.
- The outbox record uses a unique `eventId` and stores the event type, aggregate information, JSON payload, occurrence time and delivery state.
- New outbox events start in `PENDING` state with zero delivery attempts.
- The event payload contains the approved issue snapshot needed by later Inventory/Asset integrations.
- No Kafka producer or background publisher is introduced in this phase; Phase 10 owns publication of pending outbox events.
- Idempotency-Key remains optional. A replayed idempotent issue returns the existing transaction and does not enqueue another outbox event.
- Failed issue state transitions or outbox persistence failures propagate out of the transaction so the business update is not committed independently of its event record.
- The service still does not directly modify Inventory MongoDB.

## Verification status

Source-level package and Java-structure checks were performed before packaging. Run the repository Gradle commands in the target procurement-backend checkout for authoritative compilation and test verification.
