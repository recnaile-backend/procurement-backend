package com.recnaile.indent.dto.response;

import java.math.BigDecimal;

public record AutoVerifyItemResponse(
        String serialNumber,
        String itemName,
        Integer requestedQuantity,
        BigDecimal availableQuantity,
        String status,
        String message) {
}
