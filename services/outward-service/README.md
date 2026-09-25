# outward-service

Stock issue requests, approval and issue transactions.

Port: `8087`. MongoDB database: `outward`. Public API is exposed through the API Gateway under `/api/v1/outward`.

## Implemented workflow

1. Employee/authorized user creates an outward request with item, quantity, recipient and department/project.
2. Outward validates request/master-data references and requester authorization.
3. Approval is controlled by `app.authorization.approval-roles`.
4. Approval creates the issue transaction in the same MongoDB transaction as the outward state change.
5. `OUTWARD_ISSUED` is written to the transactional outbox; the Kafka publisher sends it only after the transaction has committed.
6. Inventory consumes `OUTWARD_ISSUED` and applies the AVAILABLE decrement.
7. `reusable=true` / `assetTracked=true` is carried in the event so Asset Service can create the assignment.

## API

- `POST /api/v1/outward` — create request (`Idempotency-Key` supported)
- `GET /api/v1/outward` — list requests; employees are restricted to their own requests
- `GET /api/v1/outward/mine` — current user's requests
- `GET /api/v1/outward/{outwardId}` — get request with entity authorization
- `POST /api/v1/outward/{outwardId}/approve` — approve and issue (`Idempotency-Key` supported)
- `POST /api/v1/outward/{outwardId}/reject` — reject with reason
- `POST /api/v1/outward/{outwardId}/cancel` — cancel where allowed

## State machine

`PENDING_APPROVAL -> APPROVED -> ISSUED`

`PENDING_APPROVAL -> REJECTED`

`PENDING_APPROVAL -> CANCELLED`

`APPROVED -> CANCELLED`

The APPROVED state is validated as part of the approval command and the issue transaction is created immediately in the same transaction.

## Reliability

- MongoDB transaction for outward + issue transaction + audit + outbox.
- `@Version` optimistic concurrency on the outward aggregate and issue transaction.
- Unique outward idempotency scope and unique issue transaction per outward request.
- Bounded synchronous master-data/inventory validation calls.
- Transactional outbox with retry and Kafka producer idempotence.
- No direct inventory database writes.

## Event

`OUTWARD_ISSUED` is published to `${OUTWARD_EVENTS_TOPIC:procurement.events}` using a versioned envelope. Payload includes `outwardId`, `issueTransactionId`, `itemId`, `quantity`, recipient/user, requester, issuer, approver, department/project, location, `reusable`, and correlation/trace context.

## Observability

Actuator, Prometheus metrics, Micrometer tracing/OTLP, correlation ID propagation, trace ID capture, structured audit records, and outbox metrics are included.
