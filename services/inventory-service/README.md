# inventory-service

Authoritative inventory stock ledger and balance projection.

## Responsibilities
- Consume `GOODS_RECEIVED`, `OUTWARD_ISSUED`, and `RETURN_RECORDED`.
- Maintain immutable stock movements and derived/current balances.
- Track `AVAILABLE` and `DEFECT` quantities.
- Reject operations that would make available stock negative.
- Process each event exactly once using the envelope `eventId`.
- Protect concurrent balance updates using MongoDB optimistic locking.
- Evaluate min/max thresholds and emit `LOW_STOCK_ALERT` on transition into low-stock state.
- Persist an outbox record for reliable alert publication.
- Provide reconciliation support by comparing balances with the movement ledger.
- Expose authenticated stock, threshold, and reconciliation APIs.

## API
- `GET /api/v1/inventory/stock/{itemId}`
- `PUT /api/v1/inventory/stock/{itemId}/thresholds`
- `GET /api/v1/inventory/reconciliation/{itemId}`

## Event contracts
The implementation consumes the repository contracts:
- `GOODS_RECEIVED`
- `OUTWARD_ISSUED`
- `RETURN_RECORDED`

It publishes `LOW_STOCK_ALERT` through the configured outbox topic.

## Important contract limitation
The current inbound event schemas do not provide a location on `GOODS_RECEIVED` or `RETURN_RECORDED`, and `OUTWARD_ISSUED` only provides `departmentId`. Therefore the service supports department/project/location dimensions in its balance model, but it cannot infer a distinct physical rack/location from those events unless the producing contract supplies that dimension. Missing dimensions are kept as the default/null dimension rather than invented.
