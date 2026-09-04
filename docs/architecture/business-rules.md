# Business Rules (Preserved from Source Requirements)

These rules originate from the project's business requirements and must not be silently
changed by any engineering team implementing the domain services.

## 1. Core Lifecycle

```text
Indent
  ↓
Verification (Procurement)
  ↓
Approval (TL or MD, per threshold rule below)
  ↓
Ready for PO
  ↓
Purchase Order
  ↓
Inward / GRN
  ↓
Inventory
  ↓
Outward
  ↓
Asset (for reusable items)
  ↓
Return
  ↓
Inventory / Asset update
```

## 2. Approval Threshold

```text
Indent value below configured threshold  → routed to TL for approval
Indent value at/above configured threshold → routed to MD for approval
```

The threshold is externalized configuration, not a hardcoded constant.

## 3. Inward / GRN

```text
Accepted quantity  → added to Available Stock
Rejected quantity   → added to Defect Stock
```

Both quantities are recorded against the originating PO for full traceability.

## 4. Outward

```text
Approved outward issue → Available Stock decreases by issued quantity
```

## 5. Reusable Items

```text
Outward approved (for a reusable/asset-tracked item) → Asset record created
```

An asset is tracked against the person/department it was issued to for its lifecycle.

## 6. Return

```text
Returned item in Good condition   → added back to Available Stock
Returned item in Damaged condition → added to Defect Stock
                                    → triggers Asset closure if the item was asset-tracked
```

## 7. Audit

Every one of the following actions produces an immutable audit record:

```text
Create
Update
Approve
Reject
Delete / Deactivate (where applicable)
```

Audit records are append-only — no update or delete API is ever exposed for them.

## 8. SLA & Low Stock

- **Low Stock:** when available stock for an item crosses its configured reorder
  threshold, a `LOW_STOCK_ALERT` event is emitted for notification and dashboard
  visibility.
- **SLA Escalation:** indents or approvals sitting unactioned past their configured SLA
  window emit an `SLA_BREACH` event for notification/escalation and dashboard tracking.

## 9. Roles Referenced by These Rules

```text
EMPLOYEE     — raises indents, outward requests
PROCUREMENT  — verifies indents, manages POs, inward/GRN
TL           — approves indents below threshold
MD           — approves indents above threshold
ADMIN        — master data, users, roles
```
