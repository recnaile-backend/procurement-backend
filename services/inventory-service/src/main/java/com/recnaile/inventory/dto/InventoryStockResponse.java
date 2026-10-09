package com.recnaile.inventory.dto;

import com.recnaile.inventory.domain.StockStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record InventoryStockResponse(
        String balanceKey,
        String itemId,
        String itemName,
        String category,
        String unitOfMeasure,
        String type,
        boolean assetTracked,
        BigDecimal defaultUnitPrice,
        BigDecimal gstRate,
        String barcodeQrIdentifier,
        String departmentName,
        String projectId,
        String location,
        String rack,
        String remark,
        int availableQuantity,
        int defectQuantity,
        int minimumStock,
        int maximumStock,
        boolean lowStock,
        StockStatus status,
        String masterDataStatus,
        Long version,
        Instant updatedAt) {
}
