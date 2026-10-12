# Reporting Service — Phase 2 Delivery

## Phase 2 scope: service foundation and configuration

- Retains the full Phase 1 reporting-service implementation and its API/event processing files.
- Keeps the service aligned with the root multi-project Gradle layout (`services/reporting-service`), Java 21, Spring Boot plugin conventions, and service port 8092.
- Documents configurable MongoDB, Kafka, JWT, tracing, consumer-group and topic-pattern settings.
- Uses current Spring Data MongoDB `createIndex` calls for startup index creation.
- Fixes Mockito generic matcher inference in `ReportingEventProcessorTest` by explicitly typing each matcher as `ReportingFact` or `ReportingProjection`.
- Keeps Actuator liveness/readiness probes and Prometheus endpoint configuration.

## Validation status

The ZIP structure and test matcher types were checked before packaging. A Gradle executable/wrapper and dependency cache are not available in this packaging environment, so this environment cannot independently certify a fresh Gradle test/build run. Run the commands in the root repository after replacing the service. Do not treat Docker health alone as proof that every event mapping/report is correct.

## Important integration checks

- Confirm `JWT_SECRET_KEY` algorithm/claims agree with the deployed Auth Service before production use.
- Confirm Kafka topic naming, DLT topic provisioning and consumer permissions against the root Compose/Kafka configuration.
- Reporting is a read model and must not query operational service databases directly.
- Low-stock alert events are not an authoritative inventory balance.
