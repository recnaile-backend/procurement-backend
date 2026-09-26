# Inward Service — JWT Propagation

When the Inward Service calls the Purchase Order Service during GRN creation,
it forwards the authenticated caller's `Authorization` header.

Flow:

1. Client calls Inward Service with `Authorization: Bearer <JWT>`.
2. Inward Service validates the JWT.
3. `PurchaseOrderRestClient` reads the current request's Authorization header.
4. The same header is sent to Purchase Order Service.
5. Purchase Order Service validates the JWT and serves the PO request.

If there is no active servlet request or no Authorization header, no header is added.
