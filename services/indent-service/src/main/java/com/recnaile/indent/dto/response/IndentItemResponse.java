package com.recnaile.indent.dto.response;

import java.math.BigDecimal;

public record IndentItemResponse(
        String serialNumber,
        String itemName,
        Integer quantity,
        BigDecimal estimatedUnitPrice,
        BigDecimal estimatedLineAmount,
        String remarks,
        String verificationStatus,
        String verificationMessage,
        java.time.Instant verifiedAt) {
}
