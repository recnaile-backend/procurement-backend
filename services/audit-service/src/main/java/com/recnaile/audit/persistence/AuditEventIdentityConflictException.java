package com.recnaile.audit.persistence;

/** Signals reuse of an audit event ID for a payload that differs from the stored event. */
public final class AuditEventIdentityConflictException extends IllegalStateException {
    public AuditEventIdentityConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
