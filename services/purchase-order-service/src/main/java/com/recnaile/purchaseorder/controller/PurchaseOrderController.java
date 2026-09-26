package com.recnaile.purchaseorder.controller;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderRequest;
import com.recnaile.purchaseorder.application.command.PurchaseOrderReasonRequest;
import com.recnaile.purchaseorder.common.ApiResponse;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.dto.PurchaseOrderResponse;
import com.recnaile.purchaseorder.service.PurchaseOrderCreationService;
import com.recnaile.purchaseorder.service.PurchaseOrderLifecycleService;
import com.recnaile.purchaseorder.service.PurchaseOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@Tag(name = "Purchase Orders", description = "Purchase Order creation, retrieval and lifecycle APIs")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseOrderController {

    private final PurchaseOrderCreationService creationService;
    private final PurchaseOrderQueryService queryService;
    private final PurchaseOrderLifecycleService lifecycleService;

    public PurchaseOrderController(PurchaseOrderCreationService creationService,
                                   PurchaseOrderQueryService queryService,
                                   PurchaseOrderLifecycleService lifecycleService) {
        this.creationService = creationService;
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
    }

    @Operation(
            summary = "Create a Purchase Order directly",
            description = """
                    Use this API when Procurement or MD is explicitly allowed to raise a direct/manual PO
                    without converting an approved indent. The client supplies vendorName, departmentName,
                    projectId and items. Idempotency-Key is optional; when supplied it makes retries safe and replayable. The backend validates vendorName against Master Data and generates
                    the PO identifier automatically as PO%2F26-27%2F001, PO%2F26-27%2F002, ... . Do not use this endpoint to
                    convert an approved indent; use /from-approved-indent for that workflow.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Purchase Order created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or validation failure", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Role is not permitted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Active vendor not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Master Data Service unavailable")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createManual(
            @Valid @RequestBody CreatePurchaseOrderRequest command,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {

        PurchaseOrder po = creationService.createManual(command, actorId(jwt), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PurchaseOrderResponse.from(po)));
    }

    @Operation(
            summary = "Create PO from an approved indent",
            description = """
                    Use this API in the normal procurement workflow after an indent has completed approval
                    and reached READY_FOR_PO. Idempotency-Key is optional; when supplied it makes retries safe and replayable. The client sends the indentNumber plus the PO business fields
                    (vendorName, departmentName, projectId and items). Purchase Order Service calls Indent Service
                    and automatically reads the authoritative current indent status. The user must not submit
                    indentStatus manually. The remaining PO fields are validated,
                    vendorName is resolved against active Master Data, and the backend rejects the request
                    unless the current indent status is READY_FOR_PO.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Purchase Order created from approved indent"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Indent is not READY_FOR_PO or request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Role is not permitted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indent or active vendor not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Indent already converted or idempotency conflict"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Indent Service or Master Data Service unavailable")
    })
    @PostMapping("/from-approved-indent")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createFromApprovedIndent(
            @Valid @RequestBody CreatePurchaseOrderFromApprovedIndentCommand command,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {

        PurchaseOrder po = creationService.createFromApprovedIndent(command, actorId(jwt), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PurchaseOrderResponse.from(po)));
    }

    @Operation(summary = "Get Purchase Order by ID", description = "The backend-generated Purchase Order ID is stored exactly as an encoded identifier, for example PO%2F26-27%2F001.")
    @GetMapping("/{poId}")
    public ApiResponse<PurchaseOrderResponse> getById(
            @Parameter(description = "Backend-generated PO ID exactly as stored, for example PO%2F26-27%2F001", example = "PO%2F26-27%2F001")
            @PathVariable String poId) {
        return ApiResponse.success(PurchaseOrderResponse.from(queryService.getById(poId)));
    }

    @Operation(summary = "Get all Purchase Orders", description = "Returns Purchase Orders visible to Procurement, TL and MD. Optional vendorName, projectId and departmentName filters may be supplied. If departmentName is provided, only Purchase Orders for that department are returned.")
    @GetMapping
    public ApiResponse<List<PurchaseOrderResponse>> getAll(
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String projectId,
            @Parameter(description = "Optional department name filter. It is not required. When supplied, only Purchase Orders belonging to that department are returned.")
            @RequestParam(required = false) String departmentName) {
        return ApiResponse.success(queryService.find(vendorName, projectId, departmentName)
                .stream().map(PurchaseOrderResponse::from).toList());
    }

    @Operation(
            summary = "Issue a Purchase Order",
            description = "Moves a DRAFT Purchase Order to ISSUED. A reason is mandatory and is stored with the PO for auditability.")
    @PostMapping("/{poId}/issue")
    public ApiResponse<PurchaseOrderResponse> issue(
            @PathVariable String poId,
            @Valid @RequestBody PurchaseOrderReasonRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        return transition(poId, PurchaseOrderStatus.ISSUED, request.reason(), actorId(jwt), idempotencyKey);
    }

    @Operation(
            summary = "Cancel a Purchase Order",
            description = "Cancels a Purchase Order when the lifecycle rules permit cancellation. A reason is mandatory and is stored with the PO for auditability.")
    @PostMapping("/{poId}/cancel")
    public ApiResponse<PurchaseOrderResponse> cancel(
            @PathVariable String poId,
            @Valid @RequestBody PurchaseOrderReasonRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        return transition(poId, PurchaseOrderStatus.CANCELLED, request.reason(), actorId(jwt), idempotencyKey);
    }

    @Operation(summary = "Complete a Purchase Order", description = "Moves an ISSUED or PARTIALLY_RECEIVED Purchase Order to COMPLETED according to the lifecycle state machine.")
    @PostMapping("/{poId}/complete")
    public ApiResponse<PurchaseOrderResponse> complete(
            @PathVariable String poId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        return transition(poId, PurchaseOrderStatus.COMPLETED, null, actorId(jwt), idempotencyKey);
    }


    private ApiResponse<PurchaseOrderResponse> transition(String poId,
                                                            PurchaseOrderStatus target,
                                                            String reason,
                                                            String actorId,
                                                            String idempotencyKey) {
        return ApiResponse.success(PurchaseOrderResponse.from(
                lifecycleService.transitionAndSave(poId, target, reason, actorId, idempotencyKey)));
    }

    private String actorId(Jwt jwt) {
        return jwt == null ? null : jwt.getSubject();
    }
}
