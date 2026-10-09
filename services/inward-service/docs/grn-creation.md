# GRN Creation — Phase 05

## Endpoint

`POST /api/v1/goods-receipts`

## Request

```json
{
  "poId": "PO-008",
  "deliveryNoteNumber": "DN-1001",
  "deliveryDate": "2026-09-24T10:00:00Z",
  "receivedBy": "Rajesh",
  "items": [
    {
      "itemName": "Bolt",
      "unitOfMeasure": "PCS",
      "acceptedQuantity": 5,
      "rejectedQuantity": 1,
      "rejectionReason": "Damaged"
    }
  ]
}
```

## Item matching rule

Purchase Order from the Purchase Order Service and matches each receipt item
using:

`poId + itemName`

The Purchase Order must contain a unique `itemName` for each item. If duplicate
`itemName` values are returned for one Purchase Order, GRN creation is rejected
because the receipt item would be ambiguous.

## Quantity responsibility

Phase 05 records the PO ordered quantity and the accepted/rejected quantities.
Cumulative received-quantity validation is handled in Phase 06.

### Error handling

Invalid receipt references are client/business validation errors. For example, an `itemName` that is not present in the referenced issued PO returns HTTP 400 with `ITEM_NOT_IN_PURCHASE_ORDER`; it is not exposed as an HTTP 500 server error.

The successful response exposes the GRN receipt collection as `items`, while the internal MongoDB document continues to use its existing `lines` field.


## GRN Business Number

Each created Goods Receipt Note receives a backend-generated business number such as `GRN%2F26-27%2F001`, `GRN%2F26-27%2F002`, `GRN%2F26-27%2F003`, and so on. The sequence is maintained atomically in MongoDB so concurrent GRN creation requests receive distinct numbers. The technical MongoDB `id` remains separate from the human-readable `grnId`. The financial year follows the April-March Indian financial year; for example, dates from April 2026 through March 2027 use `26-27`. The sequence restarts at `001` for each financial year. The `grnId` is stored and returned in encoded form, for example `GRN%2F26-27%2F001`, matching the Purchase Order Service.

## Phase 06/07 — Quantity Validation and Partial Receipt Tracking

For each PO item, the receipt quantity is:

`acceptedQuantity + rejectedQuantity`

The Inward Service validates that this quantity does not exceed the PO ordered
quantity, and also validates the cumulative quantity across all existing GRNs
for the same PO. This supports partial receipts while preventing over-receipt.

Example for an ordered quantity of 10:

- GRN%2F26-27%2F001 receives 6 -> remaining 4
- GRN%2F26-27%2F002 receives 4 -> remaining 0
- another receipt of 1 -> rejected with `RECEIVED_QUANTITY_EXCEEDS_ORDERED`

The same PO may have multiple GRNs, but the same `poId + deliveryNoteNumber`
combination is rejected as a duplicate. A GRN remains `DRAFT` until its own
lifecycle transition is performed; PO receipt completion is tracked separately
from the GRN lifecycle status.

## Phase 11 — Transactional Outbox

GRN persistence and its durable outbox records are written in the same MongoDB transaction. The outbox publisher retries pending/failed records and publishes them to Kafka. The outbox mechanism is used by the business event described below.

The `Idempotency-Key` header remains optional. When supplied, the idempotency record, GRN and outbox records participate in the same MongoDB transaction.

## Phase 12 — GOODS_RECEIVED Event

After a GRN is persisted as `DRAFT`, no `GOODS_RECEIVED` event is created. The event is created for each GRN receipt line only when the GRN is explicitly confirmed through `POST /api/v1/goods-receipts/{grnId}/receive`. The event uses version `1` and is published through the transactional outbox to the shared `procurement.events` Kafka topic.



See `contracts/events/GOODS_RECEIVED.schema.json` for the event contract.
