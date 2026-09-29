# Phase 11 — Audit Integration

Implemented audit integration for outward-service.

## Changes

- Added immutable `AUDIT_EVENT` payload with event identity, entity, action, actor, role, timestamp, correlation/trace ID, source service, before/after state and reason.
- Added `AuditEventService` that writes audit records through the existing transactional outbox.
- Added audit outbox creation for outward request creation, submission, approval, rejection and issue.
- Added `audit.events` Kafka topic configuration (`AUDIT_EVENTS_TOPIC`).
- Extended the outbox publisher to route `AUDIT_EVENT` to the audit topic while keeping `OUTWARD_ISSUED` on the outward topic.
- Added transactional boundaries to request creation and approval/rejection so the business change and audit outbox record commit together.
- Kept audit history append-only from outward-service; no update/delete audit API is exposed.
- Added unit coverage for audit payload creation and topic routing.

## Reliability

Audit records are created in the same Mongo transaction as the associated outward state change where the operation is transactional. Kafka delivery remains asynchronous through the outbox publisher, with the existing retry/failure handling.

## No Direct Database Access

Outward-service does not write to `auditdb`; it publishes audit events to Kafka for audit-service consumption.
