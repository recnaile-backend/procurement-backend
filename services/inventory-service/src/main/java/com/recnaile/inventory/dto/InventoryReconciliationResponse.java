package com.recnaile.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InventoryReconciliationResponse", description = "Compares current stock balances with the sum of immutable stock movement deltas.")
public record InventoryReconciliationResponse(
        String itemId,
        int balanceAvailable,
        int movementAvailable,
        int availableDifference,
        int balanceDefect,
        int movementDefect,
        int defectDifference,
        boolean availableMatches,
        boolean defectMatches,
        boolean reconciled) {
}
