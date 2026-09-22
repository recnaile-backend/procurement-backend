package com.recnaile.indent.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

public record CreateIndentRequest(

        @NotBlank
        String departmentName,

        @NotBlank
        String projectId,

        @NotBlank
        String reason,

        @NotEmpty
        @Valid
        List<ItemRequest> items
) {

    public record ItemRequest(

            @NotBlank
            String serialNumber,

            @NotBlank
            String itemName,

            @NotNull
            @Min(1)
            Integer quantity,

            @DecimalMin(value = "0.0")
            BigDecimal estimatedUnitPrice,

            String remarks
    ) {
    }
}
