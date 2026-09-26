package com.recnaile.outward.service;

import com.recnaile.outward.domain.enums.OutwardStatus;

public class InvalidOutwardStateTransitionException extends RuntimeException {

    public static final String ERROR_CODE = "OUTWARD_INVALID_STATE_TRANSITION";

    private final OutwardStatus currentStatus;
    private final OutwardStatus requestedStatus;

    public InvalidOutwardStateTransitionException(
            OutwardStatus currentStatus,
            OutwardStatus requestedStatus) {
        super("Invalid outward state transition from " + currentStatus + " to " + requestedStatus);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public OutwardStatus getCurrentStatus() { return currentStatus; }
    public OutwardStatus getRequestedStatus() { return requestedStatus; }
    public String getErrorCode() { return ERROR_CODE; }
}
