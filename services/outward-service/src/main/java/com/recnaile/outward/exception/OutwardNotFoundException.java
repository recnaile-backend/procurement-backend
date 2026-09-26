package com.recnaile.outward.exception;

public class OutwardNotFoundException extends OutwardException {
    public OutwardNotFoundException(String outwardId) {
        super("OUTWARD_NOT_FOUND", "Outward request not found: " + outwardId);
    }
}
