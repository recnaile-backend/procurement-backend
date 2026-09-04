# Procurement Platform Microservice Map

Based on the supplied Project Proposal, the platform is split into 12 business microservices plus the API Gateway.

| Service | Port | Responsibility | MongoDB DB | Gateway prefix |
|---|---:|---|---|---|
| auth-service | 8081 | Authentication, users, roles, permissions, login/token integration | auth | `/api/v1/auth/**` |
| master-data-service | 8082 | Departments, projects, vendors and item master | master_data | `/api/v1/master-data/**` |
| indent-service | 8083 | Purchase request lifecycle, verification, approval routing | indent | `/api/v1/indent/**` |
| purchase-order-service | 8084 | PO creation, supplier ordering, status and invoice metadata | purchase_order | `/api/v1/purchase-order/**` |
| inward-service | 8085 | Goods receipt/GRN, accepted/rejected quantities and vendor delivery record | inward | `/api/v1/inward/**` |
| inventory-service | 8086 | Stock ledger, availability, min/max stock and low-stock state | inventory | `/api/v1/inventory/**` |
| outward-service | 8087 | Stock issue requests, approval and issue transactions | outward | `/api/v1/outward/**` |
| return-service | 8088 | Returns, condition verification and stock adjustment | return | `/api/v1/return/**` |
| asset-service | 8089 | Reusable asset lifecycle and employee assignment | asset | `/api/v1/asset/**` |
| notification-service | 8090 | In-app/email notification orchestration and event consumers | notification | `/api/v1/notification/**` |
| audit-service | 8091 | Immutable audit trail for create/edit/approve/reject operations | audit | `/api/v1/audit/**` |
| reporting-service | 8092 | Role-based dashboards and daily/weekly/monthly/quarterly/half-yearly/yearly reporting | reporting | `/api/v1/reporting/**` |

## Business event flow

Employee -> **Indent Service** -> approval -> **Purchase Order Service** -> **Inward Service** -> **Inventory Service** -> **Outward Service** -> **Asset Service** / **Return Service**.

**Notification Service** consumes workflow events and sends notifications. **Audit Service** records all create/edit/approve/reject operations. **Reporting Service** reads/report-builds from domain events and exposes role-based dashboards.

The proposal explicitly defines automatic downstream updates for approvals, GRN receipt, stock changes, reusable asset creation/closure, low-stock alerts, SLA reminders and audit logging. fileciteturn0file0L177-L214

## Gateway routing

The gateway is the only public application endpoint. Business services are exposed internally in Kubernetes and are routable locally through Docker Compose.

## Configuration

- Local secrets are in `.env` (ignored by Git).
- `.env.example` contains placeholders.
- Kubernetes uses `mongodb-atlas-secret` for the Atlas connection.
- MongoDB Atlas is shared as a cluster, with a separate logical database per service.
- Kafka is used for asynchronous domain events.
- Redis is used by the gateway for rate limiting.

## Important

The supplied proposal is a functional specification, not an implementation; therefore these service modules provide the executable Spring Boot/MongoDB foundation and API boundaries. The business rules still need to be implemented inside each service. fileciteturn0file0L3-L11
