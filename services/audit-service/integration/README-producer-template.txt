PHASE 8 — BUSINESS-SERVICE PRODUCER INTEGRATION TEMPLATE

This template is intentionally not compiled as part of audit-service. It documents the integration boundary for each business service. The full procurement-platform service source was not present in the available input, so this ZIP does not claim to have edited producers in auth, master-data, indent, purchase-order, inward, inventory, outward, return, asset, or notification services.

REQUIRED PATTERN PER PRODUCING SERVICE
1. Create an audit event in the same local transaction as the business state change by inserting it into that service's transactional outbox.
2. Commit business data and outbox record atomically in the same service database.
3. An outbox relay publishes the envelope to `procurement.audit.events` using `eventId` as the Kafka message key and producer acknowledgements enabled.
4. Mark an outbox row as published only after broker acknowledgement; relay retries must reuse the same eventId.
5. Never publish before the business transaction commits. Never write directly to another service's database.
6. Do not include passwords, JWTs, signing secrets, credentials, or unnecessary personal data in payloads.
7. Include correlationId and traceId when available. The consumer persists the event before acknowledging its Kafka offset.
8. Validate the envelope against `src/main/resources/contracts/audit-event.schema.json` and the canonical platform contract before rollout.

TOPIC: procurement.audit.events
DLT: procurement.audit.events.dlt
MESSAGE KEY: eventId (UUID)

EXAMPLE EVENT FIELDS are defined in `src/main/resources/contracts/audit-event.schema.json` and must be reconciled with the authoritative `contracts/events/AUDIT_EVENT.schema.json` in the root repository before use. Do not treat this template as a replacement for that canonical file.
