package com.recnaile.inward.exception;

public class ItemNotInPurchaseOrderException extends IllegalArgumentException {
    public ItemNotInPurchaseOrderException(String itemId, String poId) {
        super("Item " + itemId + " does not belong to PO " + poId);
    }
}
