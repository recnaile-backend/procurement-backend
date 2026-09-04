# Resilience Patterns

Implemented (once services are built) via **Resilience4j**, configured centrally through
`libs/common-web` so every service applies the same defaults and can override per-endpoint
where justified.

## 1. Timeout

Every outbound call (REST to another service, MongoDB operation, Kafka produce) has an
explicit timeout — no call is allowed to block indefinitely. Defaults:

| Call type | Timeout |
|---|---|
| Inter-service REST (sync dependency) | 2s |
| MongoDB operation | 3s |
| Kafka produce (`delivery.timeout.ms`) | 30s (allows internal retries) |

## 2. Retry

- Applied only to **idempotent** operations (GET, or POST endpoints explicitly documented
  as safe-to-retry via an idempotency key).
- Exponential backoff with jitter — never fixed-interval retry, to avoid synchronized
  retry storms across many client instances.
- Bounded attempt count (typically 3) with a circuit breaker as the next line of defense
  if failures persist.

## 3. Circuit Breaker

- Wraps every synchronous inter-service call.
- Opens on a configured failure-rate threshold within a sliding window; while open, calls
  fail fast (returning a `503`/`FORBIDDEN`-style documented error) rather than piling up
  against an already-struggling dependency.
- Half-open probing resumes traffic gradually once the dependency recovers.

## 4. Bulkhead

- Separate thread pools / concurrency limits per downstream dependency, so a slow or
  failing dependency (e.g., a degraded MongoDB) cannot exhaust the thread pool needed to
  serve requests that don't depend on it.

## 5. Rate Limiting

- Applied at two layers: AWS WAF (coarse, perimeter) and the API Gateway
  (`RequestRateLimiter`, Redis-backed token bucket, per-client/per-route).
- Domain services may apply an additional, tighter internal rate limit on specifically
  expensive endpoints (e.g., bulk export/reporting endpoints).

## 6. Backoff & Jitter

- All retry and reconnect logic (Kafka consumer reconnects, MongoDB driver reconnects,
  REST client retries) uses exponential backoff **with jitter** to avoid thundering-herd
  reconnection storms after a shared dependency recovers.

## 7. Graceful Degradation

- Where a non-critical enrichment fails (e.g., dashboard-service failing to fetch one
  optional metric), the response degrades gracefully (omits that field, flags it as
  unavailable) rather than failing the entire request.
- Core business-critical paths (indent approval, stock deduction) do **not** degrade
  silently — a failure there is surfaced as an explicit error, never masked.

## 8. Anti-Patterns Avoided

- No blind retry of non-idempotent operations (e.g., never blindly retry "create PO"
  without an idempotency key — that would risk duplicate POs).
- No unbounded retry loops — every retry policy has a maximum attempt count.
- No synchronous call chains without a timeout/circuit breaker, ever.
