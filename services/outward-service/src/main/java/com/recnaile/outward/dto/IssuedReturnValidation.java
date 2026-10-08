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
        @Schema(description = "Quantity already returned. Return Service remains the authoritative return ledger; this boundary reports the issued quantity and the Outward item snapshot.", example = "0")
        Integer alreadyReturnedQuantity,
        @Schema(description = "Whether the issued item is reusable/asset-tracked according to the Outward snapshot derived from Master Data.", example = "true")
        boolean assetTracked) {

    public IssuedReturnValidation(String outwardId, String itemId, Integer issuedQuantity, Integer alreadyReturnedQuantity) {
        this(outwardId, itemId, issuedQuantity, alreadyReturnedQuantity, false);
    }
}
