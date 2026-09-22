# Indent Service — Phase 11.7.4

Cumulative service package containing the Phase 11.7.4 transactional outbox retry/backoff, metrics, and published-event retention work.

## Outbox retention
- PUBLISHED events older than 7 days are purged by default.
- FAILED events are retained for operator investigation/recovery.
- Configure `OUTBOX_PUBLISHED_RETENTION` to change retention.
- Configure `OUTBOX_RETENTION_CHECK_INTERVAL` to change the cleanup interval.
