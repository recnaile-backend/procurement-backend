# Notification Service — consolidated Phases 1–17

This archive is a consolidated source package based on the Phase 12 HS256/JWT version.
It intentionally preserves the in-app-only delivery model: no email provider or email credentials are added.

## Included baseline (Phases 1–12)

1. Service skeleton and API conventions.
2. Notification domain model and inbox endpoints.
3. Domain-event envelope validation.
4. Notification template catalog.
5. Event-to-notification orchestration.
6. MongoDB persistence and indexes.
7. Duplicate-safe notification writes.
8. Read/unread and inbox management.
9. Durable event inbox.
10. Event idempotency / processing ledger.
11. `empId` canonical recipient and legacy `recipientId` migration.
12. Stateless HS256 JWT security aligned with Auth Service: shared `JWT_SECRET_KEY`, issuer `auth-service`, validated `employeeId` claim mapped to the principal. Missing/blank identity is rejected.

## Consolidated hardening (Phases 13–17)

13. **Retry and DLT:** Kafka listener factory uses a bounded `DefaultErrorHandler`; exhausted messages route to `<source-topic>.dlt` (suffix configurable by `KAFKA_DLT_SUFFIX`). Invalid argument/envelope errors skip retries and go to DLT. Retry interval and retry count are configurable.
14. **Audited replay:** `POST /api/v1/notifications/operations/events/{eventId}/replay` requires `ROLE_ADMIN`, `ROLE_AUDIT_ADMIN`, or `SCOPE_notification.replay`. It publishes `NotificationEventReplayRequested` to `KAFKA_AUDIT_TOPIC` and waits for broker acknowledgement before replaying the durable inbox event. The separate Audit Service must consume and persist this topic for a durable audit trail.
15. **Delivery lifecycle:** Existing delivery status fields and states (`PENDING`, `PROCESSING`, `SENT`, `RETRYING`, `FAILED`, `DEAD_LETTERED`) remain available in the persisted model. Delivery is in-app only; no email channel is introduced.
16. **Observability:** Actuator health probes, Prometheus metrics endpoint, Micrometer Prometheus registry, OpenTelemetry tracing/export settings, and correlation/trace metadata remain configured.
17. **Production verification:** Includes a validation checklist below. A production verification phase is only complete after the full repository Gradle tests, Docker startup, Kafka retry/DLT, JWT, and Audit Service integration are executed in the user's environment.

## Required configuration

- `JWT_SECRET_KEY` must exactly match Auth Service's HS256 secret and be at least 32 UTF-8 bytes.
- `JWT_ISSUER` defaults to `auth-service`.
- `KAFKA_BOOTSTRAP_SERVERS` defaults to `kafka:29092`.
- `KAFKA_RETRY_INTERVAL_MS` defaults to `1000`.
- `KAFKA_RETRY_ATTEMPTS` defaults to `3` (retries after the first delivery).
- `KAFKA_DLT_SUFFIX` defaults to `.dlt`.
- `KAFKA_AUDIT_TOPIC` defaults to `procurement.audit.events`.
- Audit Service is separate and currently commented out in the supplied Compose file. Replay can publish the audit event to Kafka, but it cannot guarantee audit persistence until the Audit Service is active and consuming the configured topic.

## Validation commands (run from repository root)

Back up the current service folder before replacing it. Then extract this ZIP and copy `notification-service` to `services/notification-service`.

```powershell
# Destructive: run only after backing up the current service folder.
Remove-Item -Recurse -Force .\services\notification-service

.\gradlew.bat :services:notification-service:test
.\gradlew.bat :services:notification-service:build
docker compose build notification-service
docker compose up -d notification-service
docker compose ps notification-service
docker compose logs --tail=200 notification-service
```

### Phase 17 acceptance checklist

- [ ] All notification-service unit tests pass in the full multi-project repository.
- [ ] JWT signed by Auth Service is accepted; invalid signature, issuer, timestamps, and missing/blank `employeeId` are rejected.
- [ ] A recipient cannot read or mutate another employee's inbox.
- [ ] MongoDB legacy index/data migration completes without conflicting recipient identities.
- [ ] Duplicate Kafka events do not create duplicate notifications.
- [ ] A transient Kafka processing failure retries; exhausted messages appear in the configured DLT.
- [ ] Replay is denied to non-administrative authorities and emits an audit event before replay.
- [ ] Separate Audit Service consumes and persists replay audit events.
- [ ] Docker health endpoint remains healthy after startup and after Kafka/Mongo connectivity is established.
- [ ] Prometheus and OpenTelemetry are verified from the deployed Compose environment.

## Git commit

```powershell
git add services/notification-service
git commit -m "feat(notification): consolidate notification phases 1-17"
```
