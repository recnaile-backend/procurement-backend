# Outward Service Verification — Create Request

## Scope verified

- `POST /api/v1/outward` is implemented.
- `requestedBy` is derived from the authenticated Spring Security principal and is not accepted in the create payload.
- New requests start in `DRAFT`.
- Item quantities are validated as positive values.
- Recipient, department name, purpose and items are required.
- Outward numbers are allocated from an atomic MongoDB `$inc` sequence per Indian financial year.
- The persisted outward number is the encoded representation, for example `OUT%2F26-27%2F001`.
- `assetTracked` is not accepted from the client; master-data integration will determine asset classification later.
- No inventory database access, stock deduction, approval, Kafka business event, outbox, asset creation or idempotency workflow is introduced in this phase.

## Checks performed on the packaged source

Spring-managed services use explicit `@Autowired` constructor selection where test-only
clock constructors also exist, preventing ambiguous constructor selection during context startup.

- Java 21 main-source compilation with dependency stubs: PASS
- Java 21 test-source compilation with dependency stubs: PASS
- Outward-number executable smoke test: PASS
- Fiscal-year boundary test logic: PASS
- Forbidden legacy names (`OutwardRequestLine`, `departmentId`, `outwardRequestNumber`): absent
- Double-encoded `%252F` generator/source reference: absent
- API mapping `/api/v1/outward`: present
- Final ZIP re-extraction: PASS

The repository-root Gradle build must still be run with the project's own `gradlew`, because the isolated verification environment does not contain the full multi-project root wrapper/configuration.
