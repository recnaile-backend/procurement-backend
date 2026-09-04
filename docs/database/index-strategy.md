# Database Index Strategy

Indexes are created based on actual query patterns from each service's API and event
consumers — never speculatively. Each entry below documents the **why**, not just the
field.

## Cross-cutting fields common to most collections

| Field(s) | Why |
|---|---|
| `status` | Every workflow entity (indent, PO, outward request, return) is queried by its current state constantly — approval queues, dashboards, SLA monitors all filter on `status`. |
| `createdAt` | Time-range queries (dashboards, SLA breach detection, reporting) and default sort order for list endpoints. |
| `departmentId` | Role-scoped queries — a TL/employee's list views are almost always scoped to their department. |
| `projectId` | Project-level reporting and filtering, mirrored across indent/PO/outward. |
| `entityId` + `entityType` (compound, audit-service) | Audit lookups are almost always "show me the history for this specific entity," not a full scan. |
| `traceId` / `correlationId` | Support/SRE investigation queries jump straight from a trace to the record(s) involved. |

## Service-specific examples

| Service | Index | Why |
|---|---|---|
| indent-service | `{ status: 1, departmentId: 1, createdAt: -1 }` | Primary "pending approvals for my department, newest first" query. |
| po-service | `{ vendorId: 1, status: 1 }` | Vendor-scoped PO history/status lookups. |
| inward-service | `{ poId: 1 }` (unique-ish, one GRN batch per PO event) | Every GRN write and read is keyed off its originating PO. |
| inventory-service | `{ itemId: 1 }` (unique) | Stock ledger is fundamentally one document (or ledger stream) per item — every read/write is item-keyed. |
| inventory-service | `{ itemId: 1, timestamp: -1 }` on a movements collection, if ledger-style | Recomputing/auditing stock history for a specific item over time. |
| outward-service | `{ status: 1, requestedBy: 1 }` | Employee's own request history + pending-approval queues. |
| asset-service | `{ assignedTo: 1, status: 1 }` | "What assets does this person/department currently hold" — a very common lookup. |
| notification-service | `{ userId: 1, readAt: 1 }` | Unread-notifications-per-user is the dominant query. |
| audit-service | `{ entityType: 1, entityId: 1, timestamp: -1 }` | The canonical "show me this entity's full history" query. |
| dashboard-service | `{ metricKey: 1, periodStart: 1 }` on precomputed aggregates | Time-bucketed metric lookups for charts. |
| processed_events (all consumers) | `{ eventId: 1 }` unique | The idempotency guard itself — see `docs/events/idempotency-strategy.md`. |
| outbox_events (all producers) | `{ status: 1, createdAt: 1 }` | The outbox publisher's core poll query: "give me pending events, oldest first." |

## Anti-patterns explicitly avoided

- No index is created "just in case" without a named query pattern that uses it —
  unused indexes cost write throughput and storage for no benefit.
- No unbounded regex/text-search index on high-cardinality free-text fields without a
  documented search use case (dashboard-service's search endpoints, if any, get a
  purpose-built text index, not an incidental one).
- Compound indexes list fields in **equality-then-sort/range** order (per MongoDB
  index-selection guidance) — e.g., `{status: 1, createdAt: -1}` supports both the
  equality filter on `status` and the sort on `createdAt` efficiently.
