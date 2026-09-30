package com.recnaile.outward.client.masterdata;

import java.math.BigDecimal;

public record ItemResponse(
        String itemId,
        String name,
        String unitOfMeasure,
        String type,
        BigDecimal availableStock,
        boolean assetTracked,
        String status
) {
}
