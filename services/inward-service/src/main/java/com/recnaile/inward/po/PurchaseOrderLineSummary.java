package com.recnaile.inward.po;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PurchaseOrderLineSummary(
        String id,
        String itemId,
        BigDecimal quantity
) {
}
