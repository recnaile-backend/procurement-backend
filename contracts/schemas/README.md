# Shared Data Schemas

Reusable JSON Schema fragments referenced by both `contracts/events/` and
`contracts/openapi/` so a domain concept (e.g., "Money", "Address", "AuditActor") is
defined once and composed everywhere, rather than duplicated and allowed to drift.

## Files

- `money.schema.json` — a monetary amount with currency
- `pagination.schema.json` — the standard paginated list response shape (see
  `docs/api/api-standards.md` §4)
- `error.schema.json` — the standard error envelope shape (see `docs/api/api-standards.md` §3)

## Consumer-Driven Contract Testing

CI (`.github/workflows/pr-validation.yml`) validates that:

1. Every schema file in this directory and in `contracts/events/` is valid JSON Schema
   (draft-07).
2. Any schema change that removes a field, changes a field's type, or changes
   required-ness is flagged as a breaking change requiring a version bump — reviewers use
   this as a gate before merging.
