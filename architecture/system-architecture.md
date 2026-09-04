# System Architecture

## 1. Overview

The platform is a domain-driven, event-backed microservices system for Procurement &
Inventory Management. It is composed of 12 independently deployable business services
sitting behind a single API Gateway, communicating synchronously via REST for
request/response flows and asynchronously via Apache Kafka for cross-domain state
propagation.

## 2. C4 — System Context

```mermaid
C4Context
    title System Context — Procurement & Inventory Management Platform

    Person(employee, "Employee", "Raises indents, requests outward issue")
    Person(procurement, "Procurement Officer", "Verifies indents, creates POs")
    Person(tl, "Team Lead", "Approves indents below threshold")
    Person(md, "Managing Director", "Approves indents above threshold")
    Person(admin, "Admin", "Manages master data, users, roles")

    System(platform, "Procurement & Inventory Platform", "Indent-to-PO-to-Stock-to-Asset lifecycle management")

    System_Ext(mongo, "MongoDB Atlas", "Database-per-service persistence")
    System_Ext(kafka, "Apache Kafka", "Event backbone")
    System_Ext(s3, "AWS S3", "Invoice / document storage")
    System_Ext(email, "Notification channels", "Email / push / SMS providers")

    Rel(employee, platform, "Submits indents, outward requests", "HTTPS")
    Rel(procurement, platform, "Verifies indents, manages POs", "HTTPS")
    Rel(tl, platform, "Approves/rejects", "HTTPS")
    Rel(md, platform, "Approves/rejects", "HTTPS")
    Rel(admin, platform, "Configures master data", "HTTPS")

    Rel(platform, mongo, "Reads/writes", "TLS")
    Rel(platform, kafka, "Publishes/consumes events", "TLS")
    Rel(platform, s3, "Stores documents", "TLS + presigned URLs")
    Rel(platform, email, "Sends notifications", "TLS")
```

## 3. C4 — Container Diagram

```mermaid
C4Container
    title Container Diagram — Platform

    Container(gateway, "API Gateway", "Spring Cloud Gateway / Java", "TLS termination integration, JWT validation, routing, rate limiting")

    Container(auth, "auth-service", "Spring Boot / Java", "Authentication, users, roles")
    Container(master, "master-data-service", "Spring Boot / Java", "Departments, projects, vendors, items")
    Container(indent, "indent-service", "Spring Boot / Java", "Indent lifecycle")
    Container(po, "po-service", "Spring Boot / Java", "Purchase order lifecycle")
    Container(inward, "inward-service", "Spring Boot / Java", "GRN / goods receipt")
    Container(inventory, "inventory-service", "Spring Boot / Java", "Stock ledger")
    Container(outward, "outward-service", "Spring Boot / Java", "Outward issue")
    Container(ret, "return-service", "Spring Boot / Java", "Returns")
    Container(asset, "asset-service", "Spring Boot / Java", "Asset lifecycle")
    Container(notif, "notification-service", "Spring Boot / Java", "Notification dispatch")
    Container(audit, "audit-service", "Spring Boot / Java", "Immutable audit trail")
    Container(dash, "dashboard-service", "Spring Boot / Java", "Aggregated reporting")

    ContainerDb(mongo, "MongoDB Atlas", "Database-per-service")
    Container(kafka, "Apache Kafka", "Event backbone")
    Container(redis, "Redis", "Cache / locks / idempotency support")

    Rel(gateway, auth, "Routes /api/v1/auth, /api/v1/users", "HTTPS/JWT")
    Rel(gateway, master, "Routes /api/v1/departments, /projects, /vendors, /items", "HTTPS/JWT")
    Rel(gateway, indent, "Routes /api/v1/indents", "HTTPS/JWT")
    Rel(gateway, po, "Routes /api/v1/pos", "HTTPS/JWT")
    Rel(gateway, inward, "Routes /api/v1/inward", "HTTPS/JWT")
    Rel(gateway, inventory, "Routes /api/v1/stock", "HTTPS/JWT")
    Rel(gateway, outward, "Routes /api/v1/outward", "HTTPS/JWT")
    Rel(gateway, ret, "Routes /api/v1/returns", "HTTPS/JWT")
    Rel(gateway, asset, "Routes /api/v1/assets", "HTTPS/JWT")
    Rel(gateway, notif, "Routes /api/v1/notifications", "HTTPS/JWT")
    Rel(gateway, audit, "Routes /api/v1/audit", "HTTPS/JWT")
    Rel(gateway, dash, "Routes /api/v1/dashboard", "HTTPS/JWT")

    Rel(indent, kafka, "Publishes INDENT_* events")
    Rel(po, kafka, "Publishes/consumes PO_*, INDENT_APPROVED")
    Rel(inward, kafka, "Publishes GOODS_RECEIVED; consumes PO_ISSUED")
    Rel(inventory, kafka, "Consumes GOODS_RECEIVED, OUTWARD_ISSUED, RETURN_RECORDED")
    Rel(outward, kafka, "Publishes OUTWARD_ISSUED")
    Rel(ret, kafka, "Publishes RETURN_RECORDED")
    Rel(asset, kafka, "Publishes/consumes ASSET_* events")
    Rel(notif, kafka, "Consumes all domain events for notification fan-out")
    Rel(audit, kafka, "Consumes all domain events for audit trail")
    Rel(dash, kafka, "Consumes all domain events for aggregation")

    Rel(auth, mongo, "authdb")
    Rel(master, mongo, "masterdatadb")
    Rel(indent, mongo, "indentdb")
    Rel(po, mongo, "podb")
    Rel(inward, mongo, "inwarddb")
    Rel(inventory, mongo, "stockdb")
    Rel(outward, mongo, "outwarddb")
    Rel(ret, mongo, "returndb")
    Rel(asset, mongo, "assetdb")
    Rel(notif, mongo, "notificationdb")
    Rel(audit, mongo, "auditdb")
    Rel(dash, mongo, "dashboarddb")

    Rel(indent, redis, "Rate limiting / idempotency cache")
    Rel(dash, redis, "Dashboard read caching")
```

## 4. Design Principles

1. **Database-per-service** — no cross-service MongoDB access, ever.
2. **Kafka-first async** — Kafka is the only cross-domain asynchronous mechanism; it is
   never used as a synchronous RPC substitute.
3. **Gateway is infrastructure, not business logic** — authentication and coarse-grained
   routing only; authorization decisions are re-validated inside each domain service.
4. **Outbox + idempotent consumers** — no event is lost on Kafka publish failure, and no
   event is double-applied on redelivery.
5. **Everything observable** — every request carries `traceId` / `spanId` / `correlationId`
   from the gateway through to Kafka consumers.
6. **Externalized configuration** — no hardcoded secrets, credentials, or environment-specific
   URLs anywhere in source control.
7. **GitOps delivery** — Argo CD is the only actor that changes cluster state; CI never runs
   `kubectl apply` against production.

## 5. Service Responsibility Matrix

| Service | Publishes | Consumes |
|---|---|---|
| indent-service | `INDENT_SUBMITTED`, `INDENT_VERIFIED`, `INDENT_APPROVED`, `INDENT_REJECTED` | — |
| po-service | `PO_CREATED`, `PO_ISSUED`, `PO_CLOSED` | `INDENT_APPROVED` |
| inward-service | `GOODS_RECEIVED`, `REJECTION_FLAGGED` | `PO_ISSUED` |
| inventory-service | `LOW_STOCK_ALERT` | `GOODS_RECEIVED`, `OUTWARD_ISSUED`, `RETURN_RECORDED` |
| outward-service | `OUTWARD_ISSUED` | — (validates against inventory-service synchronously) |
| return-service | `RETURN_RECORDED`, `ASSET_CLOSURE_REQUESTED` | — |
| asset-service | `ASSET_CREATION_REQUESTED` (consumed), `ASSET_STATUS_CHANGED` | `OUTWARD_ISSUED`, `ASSET_CLOSURE_REQUESTED` |
| notification-service | — | all domain events |
| audit-service | — | all domain events |
| dashboard-service | — | all domain events |

See `architecture/event-driven-architecture.md` for the full event catalogue and envelope.
