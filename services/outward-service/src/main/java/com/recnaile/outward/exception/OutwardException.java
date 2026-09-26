package com.recnaile.outward.exception;

public class OutwardException extends RuntimeException {

    private final String code;

    public OutwardException(String code, String message) {
        super(message);
        this.code = code;
    }

    public OutwardException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
