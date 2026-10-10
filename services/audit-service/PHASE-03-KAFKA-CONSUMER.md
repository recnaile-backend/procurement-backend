# Phase 03 — Kafka AUDIT_EVENT Consumer

## Implemented
- Consumes the platform topic `procurement.audit.events` (override with `KAFKA_AUDIT_TOPIC`).
- Uses a dedicated consumer group `audit-service.audit-events` (override with `KAFKA_CONSUMER_GROUP`).
- Reads Kafka values as strings, parses the canonical typed `AuditEvent` envelope, and validates Jakarta Bean Validation constraints before persistence.
- Uses `enable.auto.commit=false`, `isolation.level=read_committed`, and manual acknowledgement. The offset is acknowledged only after MongoDB insertion succeeds.
- Retries listener failures three times with a 1-second fixed backoff, then publishes the failed message to `procurement.audit.events.dlt` (topic base follows `KAFKA_AUDIT_TOPIC`). Provision the `.dlt` topic before enabling the consumer in an environment where automatic topic creation is disabled.
- DLT producer uses string serialization, `acks=all`, and idempotence.

## Important boundaries
- This phase only consumes `AUDIT_EVENT`; it does not invent mappings from other domain-event schemas. Domain services must publish the canonical event using the central contract.
- A MongoDB duplicate `_id` currently follows the retry/DLT path. Duplicate delivery idempotency and duplicate-as-success handling belong to the next idempotency phase.
- The current retry policy is a fixed delay for this phase; the platform-wide `.retry` topic and exponential-backoff routing strategy must be aligned in the dedicated retry/DLT hardening phase.
- The module ZIP does not edit the repository root Compose file or topic provisioning configuration.

## Verification
Run tests/build from repository root using the commands in README. Integration verification requires reachable Kafka and MongoDB and a provisioned `procurement.audit.events` topic.
