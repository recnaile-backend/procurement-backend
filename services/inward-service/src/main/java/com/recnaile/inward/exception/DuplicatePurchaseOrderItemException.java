package com.recnaile.inward.exception;

public class DuplicatePurchaseOrderItemException extends IllegalArgumentException {
    public DuplicatePurchaseOrderItemException(String poId, String itemId) {
        super("Purchase order " + poId + " contains duplicate itemId " + itemId
                + "; itemId must be unique within a purchase order");
    }
}
