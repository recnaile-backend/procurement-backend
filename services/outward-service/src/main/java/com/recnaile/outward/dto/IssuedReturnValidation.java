package com.recnaile.outward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued outward item information used by Return Service to validate a return request.")
public record IssuedReturnValidation(
        @Schema(description = "Persisted outward number.", example = "OUT%2F26-27%2F004")
        String outwardId,
        @Schema(description = "Item identifier.", example = "Item-2")
        String itemId,
        @Schema(description = "Quantity issued for this item.", example = "2")
        Integer issuedQuantity,
        @Schema(description = "Quantity already returned. Return quantities are tracked by Return Service, so this boundary currently reports zero.", example = "0")
        Integer alreadyReturnedQuantity) {
}
