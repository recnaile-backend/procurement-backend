package com.recnaile.outward.client.inventory;

public record InventorySummaryResponse(
        String itemId,
        int availableQuantity,
        int defectQuantity,
        int totalQuantity,
        int minimumStock,
        int maximumStock,
        boolean lowStock,
        String status
) {}
