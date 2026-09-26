package com.recnaile.inward.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateGRNLineRequest(
        @NotBlank String itemId,
        @NotBlank String unitOfMeasure,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal acceptedQuantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal rejectedQuantity,
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Reason for rejected quantity. Optional. If rejectedQuantity is 0, the backend always stores null. The literal string \"null\" is also treated as null.",
                nullable = true,
                defaultValue = "null",
                example = "Damaged item"
        ) String rejectionReason
) {
    public CreateGRNLineRequest {
        if (rejectionReason != null && "null".equalsIgnoreCase(rejectionReason.trim())) {
            rejectionReason = null;
        }
    }
}
