# Outward Service

## Outward State Machine

This version adds the server-side outward lifecycle state machine on top of the
foundation and domain model.

### Lifecycle

```text
DRAFT -> PENDING_APPROVAL -> APPROVED -> ISSUED
                         \\-> REJECTED
```

`REJECTED`, `ISSUED`, and `CANCELLED` are terminal in this state machine. The
source workflow does not define a cancellation transition, so no transition to
`CANCELLED` is enabled here.

### Valid transitions

- `DRAFT -> PENDING_APPROVAL`
- `PENDING_APPROVAL -> APPROVED`
- `PENDING_APPROVAL -> REJECTED`
- `APPROVED -> ISSUED`

All other transitions are rejected with error code
`OUTWARD_INVALID_STATE_TRANSITION`.

This phase does not implement request creation, approval authorization, stock
mutation, inventory database access, Kafka events, transactional outbox,
asset creation, or idempotency processing.

### Verification

From the repository root:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:test
```

For a complete service build:

```powershell
.\\gradlew :services:outward-service:clean :services:outward-service:build
```
