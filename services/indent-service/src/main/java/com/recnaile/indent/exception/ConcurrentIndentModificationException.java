package com.recnaile.indent.exception;

public class ConcurrentIndentModificationException
        extends RuntimeException {

    public ConcurrentIndentModificationException(String message) {
        super(message);
    }

    public ConcurrentIndentModificationException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}