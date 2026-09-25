package com.recnaile.outward.exception;

public class InsufficientStockException extends OutwardException {
    public InsufficientStockException(String itemId, int requested, int available) {
        super("INSUFFICIENT_AVAILABLE_STOCK",
                "Insufficient available stock for item " + itemId +
                        ": requested=" + requested + ", available=" + available);
    }
}
