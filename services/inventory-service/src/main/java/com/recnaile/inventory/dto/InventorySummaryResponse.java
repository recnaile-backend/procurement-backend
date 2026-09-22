package com.recnaile.inventory.dto;

import com.recnaile.inventory.domain.StockStatus;

public record InventorySummaryResponse(
        String itemId,
        double availableQuantity,
        double defectQuantity,
        double totalQuantity,
        double minimumStock,
        double maximumStock,
        boolean lowStock,
        StockStatus status) {
}
