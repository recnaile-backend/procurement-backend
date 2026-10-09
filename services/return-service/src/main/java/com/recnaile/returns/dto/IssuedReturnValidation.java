package com.recnaile.returns.dto;

/**
 * Read-only validation data supplied by the Outward Service.
 *
 * outwardId is the canonical Outward number, e.g. OUT%2F26-27%2F001.
 * assetTracked is owned by Master Data/Outward and determines whether
 * Return Service must emit the Asset integration event.
 */
public record IssuedReturnValidation(
        String outwardId,
        String itemId,
        Integer issuedQuantity,
        Integer alreadyReturnedQuantity,
        Boolean assetTracked) {

    public IssuedReturnValidation(
            String outwardId,
            String itemId,
            Integer issuedQuantity,
            Integer alreadyReturnedQuantity) {
        this(outwardId, itemId, issuedQuantity, alreadyReturnedQuantity, false);
    }

    public int remainingQuantity() {
        int issued = issuedQuantity == null ? 0 : issuedQuantity;
        int returned = alreadyReturnedQuantity == null ? 0 : alreadyReturnedQuantity;
        return issued - returned;
    }
}
