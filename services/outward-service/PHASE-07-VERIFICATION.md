# Outward Service — Issue Transaction Verification

## Scope

This version adds the approved-request issue transaction workflow.

- `POST /api/v1/outward/{outwardNumber}/issue`
- Only `APPROVED` requests can be issued.
- The issuer is taken from the authenticated JWT principal.
- A dedicated MongoDB issue-transaction document is created.
- The request transitions `APPROVED -> ISSUED`.
- The issue transaction and request state update execute in one MongoDB transaction.
- A unique `outwardNumber` index prevents a second issue transaction for the same outward request.
- Inventory databases are not accessed or modified.
- `OUTWARD_ISSUED` is not published in this phase.

## Required captured data

The issue transaction stores the approved request snapshot: outward number, issued time,
items and quantities, project, requested by, department, recipient employee id/name,
issuer, approver, purpose and asset-tracked flag.

## Verification performed on the packaged archive

- Archive was extracted after packaging.
- Every Java source file was inspected for balanced delimiters.
- The current domain uses direct items, department name, and the encoded outward number field consistently.
- The issue repository, service, controller, DTO and Mongo transaction configuration are present.
- The issue endpoint path and HTTP method are present.
- The issue service requires authentication and rejects non-approved requests.
- Existing issue transaction detection prevents duplicate issue creation.
- The exact outward number representation `OUT%2F26-27%2F001` remains unchanged.
- No double-encoded percent representation is introduced by the implementation.
- Final archive was re-extracted successfully.

## Repository verification

Run from the repository root:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:test
```

Then:

```powershell
.\gradlew :services:outward-service:clean :services:outward-service:build
```

The local repository Gradle test is the final integration verification because the
complete multi-project Gradle environment is not included in this service archive.
