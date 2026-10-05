package com.recnaile.returns.dto;

import com.recnaile.returns.model.enums.ReturnCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReturnRequest(
        @NotBlank String outwardId,
        @NotBlank String itemId,
        @NotNull @Min(1) Integer quantity,
        @NotBlank @Size(max = 50) String returnByEmpId,
        @NotNull ReturnCondition condition,
        @Size(max = 2000) String remarks) {
}
