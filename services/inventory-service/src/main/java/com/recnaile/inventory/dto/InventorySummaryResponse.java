package com.recnaile.inventory.dto;

import com.recnaile.inventory.domain.StockStatus;

public record InventorySummaryResponse(
        String itemId,
        int availableQuantity,
        int defectQuantity,
        int totalQuantity,
        int minimumStock,
        int maximumStock,
        boolean lowStock,
        StockStatus status) {
}
