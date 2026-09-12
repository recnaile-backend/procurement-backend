package com.recnaile.masterdata.dto.item;

import java.math.BigDecimal;
import java.time.Instant;

import com.recnaile.masterdata.model.ItemType;
import com.recnaile.masterdata.model.MasterDataStatus;

public record ItemResponse(

        String itemId,

        String name,

        Instant createdAt,

        Instant updatedAt,

        String department,

        BigDecimal minimumStock,

        BigDecimal maximumStock,

        BigDecimal availableStock,

        BigDecimal defectStock,

        ItemType type,

        String unitOfMeasure,

        BigDecimal defaultUnitPrice,

        BigDecimal gstRate,

        BigDecimal totalPrice,

        String rack,

        MasterDataStatus status,

        String remark,

        String category,

        boolean assetTracked,

        String barcodeQrIdentifier,

        Long version

) {
}