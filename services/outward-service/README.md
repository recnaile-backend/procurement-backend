# Outward Service

## Create Outward Request

This version adds the first business command for outward requests on top of the
foundation, domain model and state machine. Spring service constructors use explicit
constructor injection so the application context has a deterministic bean wiring path.

### API

`POST /api/v1/outward`

The authenticated JWT principal supplies `requestedBy`; the client must not
supply or override that identity.

A successful request is created in `DRAFT` state and returns HTTP `201 Created`.

### Outward number

The backend allocates the next fiscal-year sequence atomically in MongoDB and
stores the encoded outward number exactly as generated, for example:

```text
OUT%2F26-27%2F001
```

The value is not encoded again by the API. The sequence is scoped to the Indian
financial year (April through March).

### Request fields

- `items`: one or more item IDs with positive quantities
- `projectId`: optional project reference
- `departmentName`: required department name
- `purpose`: required issue purpose
- `recipient`: required employee ID and employee name

`assetTracked` is not accepted from the client. Asset classification will be
resolved from master data in the later integration phase rather than trusting
client input.

### State

New requests always start at:

```text
DRAFT
```

The state machine remains responsible for later transitions.

### Boundaries

This version does not approve requests, deduct inventory, access `inventorydb`,
publish `OUTWARD_ISSUED`, create assets, implement the transactional outbox,
or implement idempotency.

### Verification

From the repository root:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:test
```

For a complete service build:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:build
```
