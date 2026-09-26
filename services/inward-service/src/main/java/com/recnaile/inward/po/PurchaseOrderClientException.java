package com.recnaile.inward.po;

public class PurchaseOrderClientException extends RuntimeException {

    public PurchaseOrderClientException(String message) {
        super(message);
    }

    public PurchaseOrderClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
