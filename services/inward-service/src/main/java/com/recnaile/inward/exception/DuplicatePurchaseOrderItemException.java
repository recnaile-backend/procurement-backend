package com.recnaile.inward.exception;

public class DuplicatePurchaseOrderItemException extends IllegalArgumentException {
    public DuplicatePurchaseOrderItemException(String poId, String itemName) {
        super("Purchase order " + poId + " contains duplicate itemName " + itemName
                + "; itemName must be unique within a purchase order");
    }
}
