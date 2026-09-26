package com.recnaile.inward.exception;

public class IdempotencyInProgressException extends RuntimeException {
    public IdempotencyInProgressException(String message) { super(message); }
}
