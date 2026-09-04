# Event Contracts

JSON Schemas for every domain event, validated against `_envelope.schema.json` (the
standard envelope, see `architecture/event-driven-architecture.md`) via `allOf`
composition. Each event's `payload` schema is authoritative for both producers and
consumers — Kafka consumers should validate incoming payloads against these schemas in
CI (contract tests) even before a Schema Registry is introduced at runtime.

| File | Event |
|---|---|
| `_envelope.schema.json` | Shared envelope — every event schema composes this |
| `INDENT_SUBMITTED.schema.json` | Employee submits an indent |
| `INDENT_VERIFIED.schema.json` | Procurement verifies an indent |
| `INDENT_APPROVED.schema.json` | TL/MD approves an indent |
| `INDENT_REJECTED.schema.json` | Indent rejected at verification or approval |
| `PO_CREATED.schema.json` | Purchase Order created |
| `PO_ISSUED.schema.json` | Purchase Order issued to vendor |
| `PO_CLOSED.schema.json` | Purchase Order closed |
| `GOODS_RECEIVED.schema.json` | GRN accepted quantity |
| `REJECTION_FLAGGED.schema.json` | GRN rejected quantity |
| `OUTWARD_ISSUED.schema.json` | Approved outward issue |
| `RETURN_RECORDED.schema.json` | Item returned |
| `ASSET_CLOSURE_REQUESTED.schema.json` | Damaged return triggers asset closure |
| `ASSET_CREATION_REQUESTED.schema.json` | Reusable-item outward triggers asset creation |
| `ASSET_STATUS_CHANGED.schema.json` | Asset lifecycle status change |
| `LOW_STOCK_ALERT.schema.json` | Item crosses reorder threshold |
| `SLA_BREACH.schema.json` | Indent/approval SLA window exceeded |

## Versioning

A breaking payload change (removed/retyped/required-ness-changed field) requires a new
schema file suffixed with the bumped version, e.g. `INDENT_APPROVED.v2.schema.json`, and
an `eventVersion: 2` in the envelope — see `architecture/event-driven-architecture.md` §7.
