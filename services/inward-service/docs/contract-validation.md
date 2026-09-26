# Event Contract Validation — Phase 15

Phase 15 adds automated JSON Schema validation for the two events emitted by inward-service.

## Contracts

- `contracts/events/GOODS_RECEIVED.schema.json`
- `contracts/events/AUDIT_EVENT.schema.json`
- `contracts/events/_envelope.schema.json`

## Tests

`ContractEventSchemaTest` invokes the real `OutboxService` methods used by the service, captures the generated outbox payload, parses it as JSON, and validates it against the corresponding contract schema.

The GOODS_RECEIVED test also explicitly verifies that `poLineId` is not present in the generated contract because that field is not part of the current Inward/GRN contract.

Run:

```powershell
.\\gradlew :services:inward-service:test
```
