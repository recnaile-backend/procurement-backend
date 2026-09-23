package com.recnaile.indent.client.inventory;

public class InventoryException extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        UNAVAILABLE,
        INVALID_RESPONSE
    }

    private final Reason reason;
    private final String referenceId;

    public InventoryException(
            Reason reason,
            String referenceId,
            String message) {
        super(message);
        this.reason = reason;
        this.referenceId = referenceId;
    }

    public InventoryException(
            Reason reason,
            String referenceId,
            String message,
            Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.referenceId = referenceId;
    }

    public Reason getReason() {
        return reason;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
