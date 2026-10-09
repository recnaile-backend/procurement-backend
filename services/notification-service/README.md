# Notification Service

In-app/email notification orchestration and event consumers.

- Port: `8090`
- MongoDB database: `notification`
- Kafka default bootstrap: `kafka:29092`
- API gateway prefix currently documented by the starter: `/api/v1/notification`

See [`docs/PHASE-01-AUDIT.md`](docs/PHASE-01-AUDIT.md) for the Phase 01 audit and implementation gaps.

**Phase 01 is audit-only.** It does not claim that the service is production-ready or that the Gradle test suite has passed. Run validation from the full repository root, where `settings.gradle`, root `build.gradle`, `gradle.properties`, `libs/`, and the Gradle wrapper are available.
