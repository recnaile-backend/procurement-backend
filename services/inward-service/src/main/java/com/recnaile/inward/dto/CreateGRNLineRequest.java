package com.recnaile.inward.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateGRNLineRequest(
        @NotBlank
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Item name exactly as carried by the issued Purchase Order. Item IDs are not used in the PO-to-GRN workflow.",
                example = "Bolt"
        )
        String itemName,
        @NotBlank
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Unit of measure for the received quantity.",
                example = "PCS"
        )
        String unitOfMeasure,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal acceptedQuantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal rejectedQuantity,
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Reason for rejected quantity. Optional. Send JSON null or omit this field when rejectedQuantity is 0. The legacy literal string \"null\" is also accepted and normalized to null.",
                nullable = true,
                example = "Damaged item"
        ) String rejectionReason
) {
    public CreateGRNLineRequest {
        if (rejectionReason != null && "null".equalsIgnoreCase(rejectionReason.trim())) {
            rejectionReason = null;
        }
    }
}
