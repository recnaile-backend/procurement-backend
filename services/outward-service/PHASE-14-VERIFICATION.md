# Phase 14 — Testing & OpenAPI Verification

## Scope

Phase 14 hardens the existing outward-service test suite and documents the public REST contract. Business workflow, event topics, persistence boundaries and service ownership are unchanged.

## OpenAPI

- JWT bearer security scheme: `bearerAuth`.
- Documented endpoints:
  - `POST /api/v1/outward`
  - `POST /api/v1/outward/{outwardNumber}/submit`
  - `POST /api/v1/outward/{outwardNumber}/approve`
  - `POST /api/v1/outward/{outwardNumber}/reject`
  - `POST /api/v1/outward/{outwardNumber}/issue`
- DTO schemas include validation-oriented descriptions and examples.
- Common HTTP outcomes are documented, including 400, 401, 403, 404, 409 and 503 where applicable.
- `Idempotency-Key` is documented as optional and replay-safe for the same outward request.
- The stored outward number convention remains URL-encoded, for example `OUT%2F26-27%2F001`.

## Testing

Existing unit tests remain in place for state transitions, request creation, approval/rejection, issue transaction, idempotency, concurrency, Master Data validation, outbox publishing and audit/notification hooks. Phase 14 adds:

- `OpenApiContractTest` for required OpenAPI metadata and endpoint documentation.
- `OutwardExceptionHandlerTest` for stable HTTP mappings of common business and downstream errors.

## Architecture

- No direct notification database access.
- No direct audit database access.
- No inventory database access.
- Notification and audit remain separate services.
- Transactional outbox and Kafka event behavior are unchanged.

## Commands

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:test
.\gradlew :services:outward-service:clean :services:outward-service:build
.\gradlew clean build
```
