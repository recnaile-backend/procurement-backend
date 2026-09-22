package com.recnaile.inventory.controller;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.service.InventoryQueryService;
import com.recnaile.inventory.service.ReconciliationService;
import com.recnaile.inventory.service.ThresholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Inventory stock, movement history, thresholds and reconciliation APIs")
public class InventoryController {
    private final InventoryQueryService query;
    private final ThresholdService threshold;
    private final ReconciliationService reconciliation;

    public InventoryController(InventoryQueryService query, ThresholdService threshold, ReconciliationService reconciliation) {
        this.query = query;
        this.threshold = threshold;
        this.reconciliation = reconciliation;
    }

    @Operation(summary = "Get stock balances", description = "Returns all stock balance projections for an item across departments, projects and locations.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock balances returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/stock/{itemId}")
    public ResponseEntity<List<StockBalanceDocument>> getStock(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId) {
        return ResponseEntity.ok(query.byItem(itemId));
    }

    @Operation(summary = "Get inventory summary", description = "Aggregates available, defect, threshold and status information for an item.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory summary returned", content = @Content(schema = @Schema(implementation = InventorySummaryResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/stock/{itemId}/summary")
    public ResponseEntity<InventorySummaryResponse> getSummary(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId) {
        return ResponseEntity.ok(query.summary(itemId));
    }

    @Operation(summary = "Get stock movement history", description = "Returns paginated immutable stock movements for an item, newest movements first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Movement history returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/stock/{itemId}/movements")
    public ResponseEntity<Page<StockMovementDocument>> getMovements(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(query.movementsByItem(itemId, pageable));
    }

    @Operation(summary = "Update stock thresholds", description = "Updates minimum and maximum stock thresholds for an item balance.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thresholds updated"),
        @ApiResponse(responseCode = "400", description = "Invalid threshold values", content = @Content),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Procurement or master-data role required", content = @Content)
    })
    @PutMapping("/stock/{itemId}/thresholds")
    public ResponseEntity<StockBalanceDocument> updateThresholds(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId,
            @Valid @RequestBody ThresholdRequest request) {
        return ResponseEntity.ok(threshold.update(itemId, request.departmentId(), request.projectId(), request.location(),
                request.minimumStock(), request.maximumStock()));
    }

    @Operation(summary = "Reconcile inventory", description = "Compares the projected stock balance with the immutable movement ledger for an item.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reconciliation result returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/reconciliation/{itemId}")
    public ResponseEntity<Map<String, Object>> reconcile(
            @Parameter(description = "Inventory item identifier", required = true, example = "ITEM-1001")
            @PathVariable String itemId) {
        return ResponseEntity.ok(reconciliation.reconcile(itemId));
    }

    public record ThresholdRequest(
            String departmentId,
            String projectId,
            String location,
            @DecimalMin(value = "0.0") double minimumStock,
            @DecimalMin(value = "0.0") double maximumStock) {
    }
}
