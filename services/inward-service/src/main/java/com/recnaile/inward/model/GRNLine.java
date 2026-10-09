package com.recnaile.inward.model;

import java.math.BigDecimal;

/** One received item row from a Purchase Order. */
public record GRNLine(
        String itemName,
        BigDecimal orderedQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        String unitOfMeasure,
        String rejectionReason
) {}
