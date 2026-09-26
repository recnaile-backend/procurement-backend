package com.recnaile.inward.exception;

public class PurchaseOrderNotFoundException extends RuntimeException {

    private final String poId;

    public PurchaseOrderNotFoundException(String poId) {
        super("Purchase Order " + poId + " was not found");
        this.poId = poId;
    }

    public String getPoId() {
        return poId;
    }
}
