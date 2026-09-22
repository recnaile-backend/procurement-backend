# Swagger ApiResponse name-collision fix

The Purchase Order service already defines `com.recnaile.purchaseorder.common.ApiResponse`
as its application response wrapper.

Swagger also defines `io.swagger.v3.oas.annotations.responses.ApiResponse`.
Java cannot import both types with the same simple name.

The controller therefore:
- keeps `com.recnaile.purchaseorder.common.ApiResponse` for API return types and `ApiResponse.success(...)`;
- uses the fully-qualified Swagger annotation for OpenAPI response documentation.

No runtime API behavior is changed by this fix.
