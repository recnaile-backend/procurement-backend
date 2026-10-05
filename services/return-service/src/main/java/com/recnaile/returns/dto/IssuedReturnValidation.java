package com.recnaile.returns.dto;

import java.math.BigDecimal;

/**
 * Read-only validation data supplied by the Outward Service for a returnable
 * issued line. The Return Service never queries the Outward Service database.
 */
public record IssuedReturnValidation(
        String outwardId,
        String outwardLineId,
        String itemId,
        BigDecimal issuedQuantity,
        BigDecimal alreadyReturnedQuantity,
        String assetId) {

    public BigDecimal remainingQuantity() {
        BigDecimal issued = issuedQuantity == null ? BigDecimal.ZERO : issuedQuantity;
        BigDecimal returned = alreadyReturnedQuantity == null ? BigDecimal.ZERO : alreadyReturnedQuantity;
        return issued.subtract(returned);
    }
}
