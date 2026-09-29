# Phase 12 Verification — Notification Hooks

## Architecture
- Outward Service publishes notification-triggering business events through the existing transactional outbox.
- Notification persistence/delivery remains owned by the separate `notification-service`; outward-service does not create notification documents or access notificationdb.
- Approval emits `OUTWARD_APPROVED`. Rejection emits `OUTWARD_REJECTED`.
- Events are published to the configurable `notification.events` Kafka topic.
- The existing outbox publisher provides retry/acknowledgement handling.

## Hooks
- Approved outward request -> requester notification hook.
- Rejected outward request -> requester notification hook with rejection reason when supplied.

## Reliability
- Notification event is written to the same transactional outbox used by the outward service.
- Kafka publication occurs asynchronously; notification failure does not roll back the originating business transaction.
- Event IDs remain unique through the existing outbox unique index.

## Separation of concerns
- `outward-service`: creates business event/outbox record only.
- `notification-service`: consumes events, deduplicates, persists notification state and delivers notifications.
- No direct cross-service database access.
