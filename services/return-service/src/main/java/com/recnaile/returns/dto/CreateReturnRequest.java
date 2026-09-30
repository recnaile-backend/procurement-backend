package com.recnaile.returns.dto;

import com.recnaile.returns.model.enums.ReturnCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateReturnRequest(
        @NotBlank String outwardId,
        @NotBlank String outwardLineId,
        @NotBlank String itemId,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
        @Size(max = 100) String assetId,
        @NotBlank @Size(max = 200) String returnedBy,
        @NotNull ReturnCondition condition,
        @Size(max = 2000) String remarks) {
}
