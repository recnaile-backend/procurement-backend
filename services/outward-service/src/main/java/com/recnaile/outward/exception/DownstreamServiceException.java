package com.recnaile.outward.exception;

public class DownstreamServiceException extends OutwardException {

    public DownstreamServiceException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
