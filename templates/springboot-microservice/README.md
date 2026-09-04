# Spring Boot Microservice Template

This is intentionally a template only. It is not a business microservice.

Use it when the organization introduces a new bounded context. Every service should follow the same package structure:

```text
src/main/java/com/company/<service>/
├── config/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── validation/
├── exception/
├── messaging/
└── client/
```

No business functionality is included in this foundation repository.
