# Phase 10 Verification — OUTWARD_ISSUED Kafka Event

## Implemented
- Durable `PENDING` outbox records from Phase 09 are published to Kafka.
- Kafka topic is configurable with `OUTWARD_ISSUED_TOPIC`; default: `outward.events`.
- Kafka message key is the exact stored `outwardNumber`.
- Kafka message value is the versioned `OUTWARD_ISSUED` JSON payload created by the transactional outbox.
- Event is marked `PUBLISHED` only after Kafka acknowledges the send.
- Failed sends retain `PENDING` status until the configured maximum attempts is reached, then become `FAILED`.
- Publisher runs on a configurable scheduled interval and processes up to 100 pending events per cycle.
- Publish timeout, polling delay and maximum attempts are configurable.
- No direct Inventory DB mutation is performed. Inventory consumes the event downstream.
- No duplicate outbox event is created by an idempotent issue replay.

## Configuration
- `OUTWARD_ISSUED_TOPIC`
- `OUTBOX_PUBLISHER_FIXED_DELAY_MS`
- `OUTBOX_PUBLISHER_SEND_TIMEOUT_MS`
- `OUTBOX_PUBLISHER_MAX_ATTEMPTS`

## Tests
- Successful Kafka acknowledgement marks the outbox event `PUBLISHED`.
- Kafka failure before the retry limit keeps the event `PENDING` and records the failure.
- Existing transactional outbox tests remain in place.

## Verification command
```powershell
.\gradlew :services:outward-service:clean :services:outward-service:test
```
