package com.recnaile.inward.po;

public class PurchaseOrderForbiddenException extends RuntimeException {

    public PurchaseOrderForbiddenException(String poId) {
        super("Purchase Order Service denied access to PO " + poId);
    }
}
