package com.recnaile.returns.dto;

/** Read-only validation data supplied by the Outward Service. */
public record IssuedReturnValidation(
        String outwardId,
        String itemId,
        Integer issuedQuantity,
        Integer alreadyReturnedQuantity) {

    public int remainingQuantity() {
        int issued = issuedQuantity == null ? 0 : issuedQuantity;
        int returned = alreadyReturnedQuantity == null ? 0 : alreadyReturnedQuantity;
        return issued - returned;
    }
}
