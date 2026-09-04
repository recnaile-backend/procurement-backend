# Structured Logging Standard

## 1. Format

JSON, one object per line, emitted to stdout (collected by the cluster's log agent and
shipped to Loki — no application ever writes directly to a log file expected to persist).

```json
{
  "timestamp": "2026-08-28T10:15:30.123Z",
  "level": "INFO",
  "service": "indent-service",
  "environment": "production",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "correlationId": "b3c1e2f0-...-uuid",
  "eventId": "b8f1...-uuid",
  "userId": "user-id-or-null",
  "requestId": "req-uuid",
  "message": "Indent approved"
}
```

Only fields applicable to the current context are populated — e.g., a pure HTTP request
log line may omit `eventId`; a Kafka consumer log line always includes it.

## 2. Implementation

Provided centrally by `libs/common-observability` as a Logback JSON encoder configuration
plus an MDC-population filter (HTTP servlet filter for request-scoped fields, Kafka
consumer interceptor for event-scoped fields) — individual services consume the shared
starter rather than hand-rolling logging configuration.

## 3. Never Logged

- Passwords (any form, including hashed — no reason to log even a hash)
- JWT access/refresh tokens
- Secrets, API keys, credentials of any kind
- Full sensitive documents (e.g., an entire invoice payload) — log a reference/id instead

Enforced via a shared log-masking filter that redacts known-sensitive field names
(`password`, `token`, `secret`, `authorization`, etc.) as a defense-in-depth backstop, in
addition to developers simply not logging these fields.

## 4. Log Levels

| Level | Usage |
|---|---|
| ERROR | Unhandled exceptions, failed business-critical operations, DLT writes |
| WARN | Degraded-but-recovered situations, retry exhaustion warnings, deprecated API usage |
| INFO | Business-significant state transitions (indent approved, PO issued, stock updated) |
| DEBUG | Detailed request/response payloads, useful only in non-production troubleshooting |

Production default: `INFO` and above; `DEBUG` enabled temporarily and scoped (per-logger,
per-namespace) during active investigation, not left on permanently.
