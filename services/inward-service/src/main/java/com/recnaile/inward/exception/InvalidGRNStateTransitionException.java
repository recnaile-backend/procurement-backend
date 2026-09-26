package com.recnaile.inward.exception;

import com.recnaile.inward.model.GRNStatus;

public class InvalidGRNStateTransitionException extends RuntimeException {

    private final GRNStatus currentStatus;
    private final GRNStatus requestedStatus;

    public InvalidGRNStateTransitionException(
            GRNStatus currentStatus,
            GRNStatus requestedStatus
    ) {
        super("Invalid GRN state transition from "
                + currentStatus + " to " + requestedStatus);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public GRNStatus getCurrentStatus() {
        return currentStatus;
    }

    public GRNStatus getRequestedStatus() {
        return requestedStatus;
    }
}
