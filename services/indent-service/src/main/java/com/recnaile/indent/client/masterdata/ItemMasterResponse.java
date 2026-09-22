package com.recnaile.indent.client.masterdata;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemMasterResponse(
        String itemId,
        String name,
        String category,
        String unitOfMeasure,
        String type,
        Boolean assetTracked,
        BigDecimal defaultUnitPrice,
        String barcodeQrIdentifier,
        BigDecimal minimumStock,
        BigDecimal maximumStock,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
