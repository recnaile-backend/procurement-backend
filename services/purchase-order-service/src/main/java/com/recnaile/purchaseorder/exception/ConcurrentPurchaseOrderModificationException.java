package com.recnaile.purchaseorder.exception;

public class ConcurrentPurchaseOrderModificationException extends RuntimeException {

    public ConcurrentPurchaseOrderModificationException(String message) {
        super(message);
    }
}
