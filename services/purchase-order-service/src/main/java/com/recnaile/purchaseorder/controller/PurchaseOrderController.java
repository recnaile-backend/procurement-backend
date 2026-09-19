package com.recnaile.purchaseorder.controller;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.common.ApiResponse;
import com.recnaile.purchaseorder.controller.dto.PurchaseOrderResponse;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import com.recnaile.purchaseorder.service.PurchaseOrderCreationService;
import com.recnaile.purchaseorder.service.PurchaseOrderLifecycleService;
import com.recnaile.purchaseorder.service.PurchaseOrderQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final PurchaseOrderRepository repository;

    public PurchaseOrderController(PurchaseOrderCreationService creationService,
                                   PurchaseOrderQueryService queryService,
                                   PurchaseOrderLifecycleService lifecycleService,
                                   PurchaseOrderRepository repository) {
        this.creationService = creationService;
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
        this.repository = repository;
    }

    @PostMapping({"", "/from-approved-indent"})
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createFromApprovedIndent(
            @Valid @RequestBody CreatePurchaseOrderFromApprovedIndentCommand command,
            @AuthenticationPrincipal Jwt jwt) {
        PurchaseOrder po = creationService.createFromApprovedIndent(command, actorId(jwt));
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
                .stream().map(PurchaseOrderResponse::from).toList());
    }

    @PostMapping("/{poId}/issue")
    public ApiResponse<PurchaseOrderResponse> issue(@PathVariable String poId, @AuthenticationPrincipal Jwt jwt) {
        return transitionAndSave(poId, PurchaseOrderStatus.ISSUED, actorId(jwt));
    }

    @PostMapping("/{poId}/cancel")
    public ApiResponse<PurchaseOrderResponse> cancel(@PathVariable String poId, @AuthenticationPrincipal Jwt jwt) {
        return transitionAndSave(poId, PurchaseOrderStatus.CANCELLED, actorId(jwt));
    }

    @PostMapping("/{poId}/complete")
    public ApiResponse<PurchaseOrderResponse> complete(@PathVariable String poId, @AuthenticationPrincipal Jwt jwt) {
        return transitionAndSave(poId, PurchaseOrderStatus.COMPLETED, actorId(jwt));
    }

    @PostMapping("/{poId}/close")
    public ApiResponse<PurchaseOrderResponse> close(@PathVariable String poId, @AuthenticationPrincipal Jwt jwt) {
        return transitionAndSave(poId, PurchaseOrderStatus.CLOSED, actorId(jwt));
    }

    private ApiResponse<PurchaseOrderResponse> transitionAndSave(String poId,
                                                                   PurchaseOrderStatus target,
                                                                   String actorId) {
        PurchaseOrder po = queryService.getById(poId);
        lifecycleService.transition(po, target);
        po.setUpdatedBy(actorId);
        return ApiResponse.success(PurchaseOrderResponse.from(repository.save(po)));
    }

    private String actorId(Jwt jwt) {
        return jwt == null ? null : jwt.getSubject();
    }
}
