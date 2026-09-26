package com.recnaile.inward.exception;

public class PurchaseOrderNotEligibleException extends RuntimeException {

    private final String poId;
    private final String currentStatus;

    public PurchaseOrderNotEligibleException(String poId, String currentStatus) {
        super("Purchase Order " + poId
                + " cannot be used for goods receipt because its current status is "
                + currentStatus + ". The Purchase Order must be ISSUED before a GRN can be created.");
        this.poId = poId;
        this.currentStatus = currentStatus;
    }

    public String getPoId() {
        return poId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
