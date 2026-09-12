package com.recnaile.masterdata.dto.item;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.recnaile.masterdata.model.ItemType;

public record CreateItemRequest(

        @NotBlank
        @Size(max = 100)
        String itemId,

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 100)
        String category,

        @NotBlank
        @Size(max = 50)
        String unitOfMeasure,

        @NotNull
        ItemType type,

        boolean assetTracked,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal defaultUnitPrice,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal gstRate,

        @Size(max = 100)
        String barcodeQrIdentifier,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal minimumStock,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal maximumStock,

        @Size(max = 100)
        String department,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal availableStock,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal defectStock,

        @Size(max = 100)
        String rack,

        @Size(max = 500)
        String remark
) {
}