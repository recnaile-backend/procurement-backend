package com.recnaile.outward.exception;

public class IdempotencyInProgressException extends OutwardException {
    public IdempotencyInProgressException() {
        super("IDEMPOTENCY_IN_PROGRESS",
                "An identical command is already being processed.");
    }
}
