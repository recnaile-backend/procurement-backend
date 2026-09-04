# Java Spring Boot Standard

All backend applications use Java 21 and Spring Boot 3.x with Gradle Groovy DSL.

- No Kotlin source files or Kotlin Gradle DSL are used.
- Every domain service is a standalone Spring Boot application under `services/`.
- Shared cross-cutting concerns live under `libs/` and contain no domain business logic.
- The API Gateway is Spring Cloud Gateway and is responsible for routing, authentication enforcement, correlation IDs, CORS, rate limiting, resilience and observability only.
- Domain business rules must remain inside their owning service.
