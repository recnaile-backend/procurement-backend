package com.recnaile.inventory.masterdata;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MasterDataItemResponse(
        String itemId,
        String name,
        String createdAt,
        String updatedAt,
        String department,
        BigDecimal minimumStock,
        BigDecimal maximumStock,
        BigDecimal availableStock,
        BigDecimal defectStock,
        String type,
        String unitOfMeasure,
        BigDecimal defaultUnitPrice,
        BigDecimal gstRate,
        BigDecimal totalPrice,
        String rack,
        String status,
        String remark,
        String category,
        boolean assetTracked,
        String barcodeQrIdentifier,
        Long version) {
}
