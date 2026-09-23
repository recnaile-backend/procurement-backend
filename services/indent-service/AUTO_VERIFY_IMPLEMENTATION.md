# Auto Verify — Master Data Item Verification

## Purpose

Auto Verify checks every item in an indent against the Item Master list exposed by Master Data Service.

Create Indent does **not** validate item existence. Procurement performs the item verification later through Auto Verify.

## Flow

```text
POST /api/v1/indents/{indentNumber}/auto-verify
        |
        v
Indent Service loads the indent
        |
        v
GET /api/v1/master-data/items
        |
        v
Compare every indent item's itemName with Item Master name
        |
        +--> active matching item    -> AVAILABLE
        |
        +--> missing/inactive item   -> NOT_AVAILABLE
```

## Per-item persistence

The result is persisted on each `IndentItem`:

- `verificationStatus`
- `verificationMessage`
- `verifiedAt`

The main indent workflow status is not changed by Auto Verify.

## Matching

Item names are compared after trimming whitespace and ignoring case.

An Item Master record is considered available when a matching record exists and its status is `ACTIVE`.

## Response

The API returns one result for every item in the indent. `availableQuantity` is `null` because Master Data Item Master is being used for item existence/active-status verification; it does not provide live inventory quantity.

## Create Indent

Create remains a request operation:

- no Item Master lookup
- no Inventory Service lookup
- Procurement validates the item during verification
