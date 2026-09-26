package com.recnaile.outward.exception;

public class IdempotencyConflictException extends OutwardException {
    public IdempotencyConflictException() {
        super("IDEMPOTENCY_KEY_CONFLICT",
                "The Idempotency-Key was already used with a different request payload.");
    }
}
