# Reporting Service Implementation Status

## Implemented in this delivery

- Replaced generic record CRUD with typed current projections, immutable event facts, and typed daily snapshots.
- Added Kafka topic-pattern consumer, envelope checks, duplicate-event handling, event-order guard, retries and dead-letter publishing.
- Added dashboard counts, pending-stage aging, PO lifecycle counts, inward accepted/rejected quantity totals, outward/return quantities, observed low-stock alert data, asset counts, SLA breach counts, freshness metadata, period buckets and daily snapshots.
- Added pageable projection/fact/snapshot APIs with filter validation and sort allow-list.
- Added role checks, HS256 JWT validation aligned with the supplied Auth Service implementation, and department/project scope enforcement when scope claims are present.
- Added MongoDB indexes, Micrometer counters, OpenAPI UI dependency, and processor unit tests.

## Explicit constraints / not yet production-verified

- The supplied event schemas do not define an authoritative full inventory balance/snapshot event. Stock fields are therefore labelled as observed low-stock alert data and must not be treated as total current stock.
- Kafka topic naming and DLT topic provisioning must be checked against the root repository's actual topic configuration.
- Projection rebuild is documented as an operational replay procedure; a production-safe automated replay/cutover workflow remains to be implemented and tested.
- Department/project authorization claim names must be verified against the current Auth Service token. Unscoped TL tokens are denied for scoped report endpoints.
- Financial-year boundaries are not inferred. Period buckets use UTC calendar periods.
- Build and tests were not executed in this environment because a Gradle executable/wrapper and dependency cache were not available alongside the uploaded service-only archive. Run the verification commands in the full repository before merging.
