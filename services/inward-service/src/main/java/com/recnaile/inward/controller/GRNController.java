package com.recnaile.inward.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recnaile.inward.dto.ApiResponse;
import com.recnaile.inward.dto.CreateGRNRequest;
import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.service.GRNCreationService;
import com.recnaile.inward.service.GRNQueryService;
import com.recnaile.inward.service.GRNReceiptService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/goods-receipts")
@Tag(name = "Goods Receipts", description = "Goods Receipt Note / Inward APIs")
public class GRNController {

    private final GRNCreationService grnCreationService;
    private final GRNQueryService grnQueryService;
    private final GRNReceiptService grnReceiptService;

    public GRNController(GRNCreationService grnCreationService, GRNQueryService grnQueryService, GRNReceiptService grnReceiptService) {
        this.grnCreationService = grnCreationService;
        this.grnQueryService = grnQueryService;
        this.grnReceiptService = grnReceiptService;
    }

    @Operation(
            summary = "List Goods Receipt Notes",
            description = "Returns all GRNs. The optional poId parameter limits the result to GRNs belonging to that Purchase Order. "
                    + "When poId is omitted, all GRNs are returned. No pagination or status filter is supported by this API."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "GRNs returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid poId parameter",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"INVALID_GRN_REQUEST\",\"message\":\"Invalid poId\",\"traceId\":\"corr-123\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user is not permitted to access GRNs"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"An unexpected error occurred\",\"traceId\":\"corr-123\"}}")
                    )
            )
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TL', 'MD', 'PROCUREMENT')")
    public ResponseEntity<ApiResponse<List<GRNResponse>>> list(
            @Parameter(description = "Optional Purchase Order ID. Omit this parameter to return all GRNs.", example = "PO-008")
            @RequestParam(required = false) String poId
    ) {
        return ResponseEntity.ok(ApiResponse.success(grnQueryService.list(poId)));
    }

    @Operation(
            summary = "Get GRN by GRN ID",
            description = "Returns a single Goods Receipt Note using its backend-generated business GRN identifier. In Swagger UI, enter the identifier as GRN/26-27/001; Swagger will URL-encode the slashes to GRN%2F26-27%2F001. The stored and returned business identifier remains GRN%2F26-27%2F001."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "GRN returned successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user is not permitted to access GRNs"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "GRN not found",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"GRN_NOT_FOUND\",\"message\":\"Goods receipt GRN%2F26-27%2F001 was not found\",\"traceId\":\"corr-123\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"An unexpected error occurred\",\"traceId\":\"corr-123\"}}")
                    )
            )
    })
    @GetMapping("/{grnId}")
    @io.swagger.v3.oas.annotations.Parameter(
            name = "grnId",
            description = "Enter the GRN identifier using slashes, for example GRN/26-27/001. Swagger encodes the slashes for the HTTP request.",
            example = "GRN/26-27/001",
            required = true
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TL', 'MD', 'PROCUREMENT')")
    public ResponseEntity<ApiResponse<GRNResponse>> getByGrnId(@PathVariable String grnId) {
        return ResponseEntity.ok(ApiResponse.success(grnQueryService.getByGrnId(grnId)));
    }

    @Operation(
            summary = "Create GRN",
            description = "Creates a DRAFT GRN against an ISSUED Purchase Order. "
                    + "The Purchase Order Service is revalidated before the GRN is persisted. "
                    + "Each requested itemName must match an itemName carried by the issued PO. Rejected quantity requires a rejection reason. "
                    + "Cumulative outstanding-quantity validation is handled in Phase 06."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "GRN created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request, item not on PO, duplicate item, or missing rejection reason",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ItemNotInPurchaseOrder",
                                            value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"ITEM_NOT_IN_PURCHASE_ORDER\",\"message\":\"Item Bolt-999 does not belong to PO PO-008\",\"traceId\":\"corr-123\"}}"
                                    ),
                                    @ExampleObject(
                                            name = "ValidationError",
                                            value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"poId: must not be blank\",\"traceId\":\"corr-123\"}}"
                                    ),
                                    @ExampleObject(
                                            name = "RejectionReasonRequired",
                                            value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"REJECTION_REASON_REQUIRED\",\"message\":\"rejectionReason is required when rejectedQuantity is greater than zero\",\"traceId\":\"corr-123\"}}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user is not permitted to create a GRN",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"You do not have permission to create a GRN. Required role: PROCUREMENT or MD.\",\"traceId\":\"corr-123\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Purchase Order exists but is not eligible for receipt",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"PURCHASE_ORDER_NOT_ISSUED\",\"message\":\"Purchase Order PO-008 cannot be used for goods receipt because its current status is DRAFT. The Purchase Order must be ISSUED before a GRN can be created.\",\"traceId\":\"corr-123\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Idempotency key conflict or an identical request is already in progress",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"IDEMPOTENCY_KEY_CONFLICT\",\"message\":\"The Idempotency-Key has already been used with a different request.\",\"traceId\":\"corr-123\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "Purchase Order Service could not be queried successfully",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"PURCHASE_ORDER_SERVICE_ERROR\",\"message\":\"Purchase Order Service returned an unsuccessful response\",\"traceId\":\"corr-123\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            schema = @Schema(implementation = com.recnaile.inward.dto.ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"success\":false,\"data\":null,\"error\":{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"An unexpected error occurred\",\"traceId\":\"corr-123\"}}")
                    )
            )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    public ResponseEntity<ApiResponse<GRNResponse>> createGRN(
            @Valid @RequestBody CreateGRNRequest request,
            @Parameter(
                    description = "Optional idempotency key. When supplied, retrying the same request with the same key returns the original GRN instead of creating a duplicate. If omitted, the request is processed normally.",
                    required = false,
                    example = "6f4d2b5c-4b8f-4a2e-9f9b-123456789abc")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        String principal = authentication == null ? "UNKNOWN" : authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(grnCreationService.create(request, idempotencyKey, principal)));
    }
    @Operation(
            summary = "Confirm GRN receipt",
            description = "Confirms a DRAFT GRN. Inventory-facing GOODS_RECEIVED events are created only after this confirmation. The final status is PARTIALLY_RECEIVED when the PO still has outstanding quantity, or COMPLETED when the PO is fully received."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "GRN receipt confirmed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Authenticated user is not permitted to confirm a GRN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GRN not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Invalid GRN state or concurrent update")
    })
    @PostMapping("/{grnId}/receive")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    public ResponseEntity<ApiResponse<GRNResponse>> receiveGRN(
            @PathVariable String grnId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        String principal = authentication == null ? "UNKNOWN" : authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(
                grnReceiptService.receive(grnId, idempotencyKey, principal)));
    }

}
