package com.recnaile.returns.exception;

import com.recnaile.returns.model.enums.ReturnStatus;

/**
 * Raised when a return lifecycle transition is not permitted by the Return state machine.
 */
public class InvalidReturnStateTransitionException extends RuntimeException {

    private final ReturnStatus currentStatus;
    private final ReturnStatus requestedStatus;

    public InvalidReturnStateTransitionException(ReturnStatus currentStatus, ReturnStatus requestedStatus) {
        super("Invalid return state transition from " + currentStatus + " to " + requestedStatus);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public ReturnStatus getCurrentStatus() {
        return currentStatus;
    }

    public ReturnStatus getRequestedStatus() {
        return requestedStatus;
    }
}
