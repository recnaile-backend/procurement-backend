package com.recnaile.inward.exception;

public class ItemNotInPurchaseOrderException extends IllegalArgumentException {
    public ItemNotInPurchaseOrderException(String itemName, String poId) {
        super("Item " + itemName + " does not belong to PO " + poId);
    }
}
