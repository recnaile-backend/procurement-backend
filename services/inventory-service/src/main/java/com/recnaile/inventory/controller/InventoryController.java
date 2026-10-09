package com.recnaile.inventory.controller;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.dto.ApiErrorResponse;
import com.recnaile.inventory.dto.InventoryAdjustmentRequest;
import com.recnaile.inventory.dto.InventoryStockResponse;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.dto.InventoryReconciliationResponse;
import com.recnaile.inventory.service.InventoryQueryService;
import com.recnaile.inventory.service.InventoryAdjustmentService;
import com.recnaile.inventory.service.ReconciliationService;
import com.recnaile.inventory.service.ThresholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Inventory stock, movement history, thresholds and reconciliation APIs")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {
    private final InventoryQueryService query;
    private final ThresholdService threshold;
    private final ReconciliationService reconciliation;
    private final InventoryAdjustmentService adjustment;

    public InventoryController(InventoryQueryService query, ThresholdService threshold, ReconciliationService reconciliation, InventoryAdjustmentService adjustment) {
        this.query = query;
        this.threshold = threshold;
        this.reconciliation = reconciliation;
        this.adjustment = adjustment;
    }

    @Operation(
        summary = "Get all stock balances",
        description = "Returns all current stock balance projections across all inventory items. No query parameters are required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All stock balances returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/stock")
    public ResponseEntity<List<InventoryStockResponse>> getAllStocks() {
        return ResponseEntity.ok(query.allStocks());
    }

    @Operation(summary = "Get stock balances", description = "Returns all stock balance projections for an item across departments, projects and locations.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock balances returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/stock/{itemId}")
    public ResponseEntity<List<InventoryStockResponse>> getStock(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId) {
        return ResponseEntity.ok(query.byItem(itemId));
    }

    @Operation(summary = "Get inventory summary", description = "Aggregates available, defect, threshold and status information for an item.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory summary returned", content = @Content(schema = @Schema(implementation = InventorySummaryResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/stock/{itemId}/summary")
    public ResponseEntity<InventorySummaryResponse> getSummary(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId) {
        return ResponseEntity.ok(query.summary(itemId));
    }

    @Operation(
        summary = "Get stock movement history",
        description = "Returns immutable stock movements for an item, newest movements first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Movement history returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/stock/{itemId}/movements")
    public ResponseEntity<List<StockMovementDocument>> getMovements(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId) {
        return ResponseEntity.ok(query.movementsByItem(itemId));
    }

    @Operation(
        summary = "Apply administrative stock adjustment",
        description = "Controlled Procurement/MD correction. The normal stock path is event-driven Inward, Outward and Return processing. This endpoint records an explicit signed movement and requires Idempotency-Key.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock adjustment applied"),
        @ApiResponse(responseCode = "400", description = "Invalid adjustment or missing Idempotency-Key", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Procurement or master-data role required", content = @Content),
        @ApiResponse(responseCode = "409", description = "Concurrent or insufficient-stock conflict", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @org.springframework.web.bind.annotation.PostMapping("/stock/{itemId}/adjustments")
    public ResponseEntity<InventoryStockResponse> applyAdjustment(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId,
            @Valid @RequestBody InventoryAdjustmentRequest request,
            Authentication authentication,
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey) {
        String actorId = authentication == null ? "inventory-api" : authentication.getName();
        StockBalanceDocument balance = adjustment.applyAdjustment(
                itemId, request.departmentName(), request.projectId(), request.location(),
                request.stockType(), request.delta(), request.reason(), actorId, idempotencyKey);
        return ResponseEntity.ok(query.byItem(itemId).stream()
                .filter(r -> r.balanceKey().equals(balance.getBalanceKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Updated inventory balance could not be projected")));
    }

    @Operation(summary = "Update stock thresholds", description = "Updates minimum and maximum stock thresholds for an item balance.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thresholds updated"),
        @ApiResponse(responseCode = "400", description = "Invalid threshold values",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Procurement or master-data role required", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/stock/{itemId}/thresholds")
    public ResponseEntity<InventoryStockResponse> updateThresholds(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId,
            @Valid @RequestBody ThresholdRequest request,
            Authentication authentication) {
        String actorId = authentication == null ? "inventory-api" : authentication.getName();
        StockBalanceDocument updated = threshold.update(itemId, request.departmentName(), request.projectId(), request.location(),
                request.minimumStock(), request.maximumStock(), actorId);
        return ResponseEntity.ok(query.byItem(itemId).stream()
                .filter(r -> r.balanceKey().equals(updated.getBalanceKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Updated inventory balance could not be projected")));
    }

    @Operation(summary = "Reconcile inventory", description = "Compares the projected stock balance with the immutable movement ledger for an item.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reconciliation result returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/reconciliation/{itemId}")
    public ResponseEntity<InventoryReconciliationResponse> reconcile(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId) {
        return ResponseEntity.ok(reconciliation.reconcile(itemId));
    }

    @Schema(name = "ThresholdRequest", description = "Stock threshold configuration for an item balance.")
    public record ThresholdRequest(
            @Schema(description = "Department name dimension. Optional.", example = "IT")
            String departmentName,

            @Schema(description = "Project dimension. Optional.", example = "PROJ-2001")
            String projectId,

            @Schema(description = "Physical/location dimension. Optional.", example = "RACK-A01")
            String location,

            @Min(value = 0, message = "minimumStock must be >= 0")
            @Schema(description = "Minimum stock threshold.", minimum = "0", example = "10")
            int minimumStock,

            @Min(value = 0, message = "maximumStock must be >= 0")
            @Schema(description = "Maximum stock threshold. Must be greater than or equal to minimumStock.", minimum = "0", example = "50")
            int maximumStock) {
    }
}
