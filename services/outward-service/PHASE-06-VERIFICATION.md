# Outward Service — Approval Workflow & RBAC Verification

## Scope

This version adds authenticated submit/approve/reject workflow operations and configurable approval-role authorization. It does not deduct inventory, create issue transactions, publish `OUTWARD_ISSUED`, or modify `inventorydb`.

## Endpoints

- `POST /api/v1/outward/{outwardNumber}/submit`
- `POST /api/v1/outward/{outwardNumber}/approve`
- `POST /api/v1/outward/{outwardNumber}/reject`

## Rules

- Only the authenticated requester can submit their own `DRAFT` request.
- Approval and rejection require a configured approval role.
- Default approval roles are `TL,MD`; configure with `OUTWARD_APPROVAL_ROLES`.
- JWT roles are read from the `roles` claim and mapped to `ROLE_*` authorities.
- State transitions are enforced by the existing state machine.
- `approvedBy`/`approvedAt` are populated from the authenticated approver.
- `rejectedBy`/`rejectedAt`/`rejectionReason` are populated on rejection.
- MongoDB optimistic locking failures return a stable conflict error.
- The service never modifies inventory stock during approval.

## Security

JWT resource-server validation uses the configured JWK Set URI. Issuer validation is enabled when `APP_SECURITY_JWT_ISSUER_URI` is configured.

## Verification performed before packaging

The approval-service unit-test fixture includes all required response fields (`items`, `departmentName`, `purpose`, and `recipient`) so the service response mapper is exercised with a valid `OutwardRequest` shape.

- All Java source files inspected for stale Phase 02/03 references.
- No `OutwardRequestLine`, `departmentId`, or `outwardRequestNumber` references remain.
- No `%252F` double-encoded outward number implementation remains.
- Security error JSON escaping checked.
- Approval policy and service tests added.
- Final archive is re-extracted and re-audited.

The repository-root Gradle test must still be run in the user's complete multi-project repository because this package alone does not contain the root Gradle wrapper.
