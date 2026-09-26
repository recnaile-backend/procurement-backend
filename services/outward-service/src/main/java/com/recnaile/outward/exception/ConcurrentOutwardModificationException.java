package com.recnaile.outward.exception;

public class ConcurrentOutwardModificationException extends OutwardException {
    public ConcurrentOutwardModificationException() {
        super("OUTWARD_CONCURRENCY_CONFLICT",
                "The outward request was modified by another request. Please refresh and retry.");
    }
}
