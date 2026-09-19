package com.recnaile.purchaseorder.controller;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.common.ApiResponse;
import com.recnaile.purchaseorder.dto.PurchaseOrderResponse;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.service.PurchaseOrderCreationService;
import com.recnaile.purchaseorder.service.PurchaseOrderLifecycleService;
import com.recnaile.purchaseorder.service.PurchaseOrderQueryService;
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

    @PostMapping({"", "/from-approved-indent"})
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createFromApprovedIndent(
            @Valid @RequestBody CreatePurchaseOrderFromApprovedIndentCommand command,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {

        PurchaseOrder po = creationService.createFromApprovedIndent(
                command,
                actorId(jwt),
                idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PurchaseOrderResponse.from(po)));
    }

    @GetMapping("/{poId}")
    public ApiResponse<PurchaseOrderResponse> getById(@PathVariable String poId) {
        return ApiResponse.success(PurchaseOrderResponse.from(queryService.getById(poId)));
    }

    @GetMapping
    public ApiResponse<List<PurchaseOrderResponse>> getAll(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String projectId) {
        return ApiResponse.success(queryService.find(status, vendorId, projectId)
                .stream()
                .map(PurchaseOrderResponse::from)
                .toList());
    }

    @PostMapping("/{poId}/issue")
    public ApiResponse<PurchaseOrderResponse> issue(
            @PathVariable String poId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        return transition(poId, PurchaseOrderStatus.ISSUED, actorId(jwt), idempotencyKey);
    }

    @PostMapping("/{poId}/cancel")
    public ApiResponse<PurchaseOrderResponse> cancel(
            @PathVariable String poId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        return transition(poId, PurchaseOrderStatus.CANCELLED, actorId(jwt), idempotencyKey);
    }

    @PostMapping("/{poId}/complete")
    public ApiResponse<PurchaseOrderResponse> complete(
            @PathVariable String poId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        return transition(poId, PurchaseOrderStatus.COMPLETED, actorId(jwt), idempotencyKey);
    }

    @PostMapping("/{poId}/close")
    public ApiResponse<PurchaseOrderResponse> close(
            @PathVariable String poId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        return transition(poId, PurchaseOrderStatus.CLOSED, actorId(jwt), idempotencyKey);
    }

    private ApiResponse<PurchaseOrderResponse> transition(String poId,
                                                           PurchaseOrderStatus target,
                                                           String actorId,
                                                           String idempotencyKey) {
        return ApiResponse.success(PurchaseOrderResponse.from(
                lifecycleService.transitionAndSave(poId, target, actorId, idempotencyKey)));
    }

    private String actorId(Jwt jwt) {
        return jwt == null ? null : jwt.getSubject();
    }
}
