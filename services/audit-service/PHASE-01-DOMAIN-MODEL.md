# Phase 01 — Audit domain model and event contract

## Scope

This phase adds typed Java records for the existing `AUDIT_EVENT` envelope and payload. The canonical JSON Schemas remain in the repository root at:

- `contracts/events/_envelope.schema.json`
- `contracts/events/AUDIT_EVENT.schema.json`

The model uses the existing contract field names and required fields. `eventType` is constrained to `AUDIT_EVENT`; `eventVersion` must be positive; required payload values are validated. Optional schema fields remain nullable. `actorRoles` is defensively copied.

## Deliberately out of scope

This phase does not yet wire the typed event model into the existing CRUD controller, implement Kafka ingestion, persist these events, add deduplication, change the public API, or implement RBAC. Those belong to later roadmap phases. The legacy CRUD controller and `RecordDocument` are retained temporarily so this phase does not break the existing service while the domain model is introduced.

## Verification

Run from the full repository root:

```powershell
.\gradlew.bat :services:audit-service:test
.\gradlew.bat :services:audit-service:build
```

The included `AuditEventContractTest` verifies a valid event, fixed event type, positive version, required payload fields, and defensive copying of roles.
