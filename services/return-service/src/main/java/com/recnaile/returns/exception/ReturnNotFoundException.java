package com.recnaile.returns.exception;

public class ReturnNotFoundException extends RuntimeException {
    public ReturnNotFoundException(String returnId) {
        super("Return request not found: " + returnId);
    }
}
