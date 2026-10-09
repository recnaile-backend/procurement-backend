package com.recnaile.outward.client.masterdata;

import java.math.BigDecimal;

public record ItemResponse(
        String itemId,
        String name,
        String unitOfMeasure,
        String type,
        /** Legacy field retained for wire compatibility; Outward never uses it for stock decisions. */
        @Deprecated BigDecimal availableStock,
        boolean assetTracked,
        String status
) {
}
