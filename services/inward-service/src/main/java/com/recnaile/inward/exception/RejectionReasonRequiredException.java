package com.recnaile.inward.exception;

public class RejectionReasonRequiredException extends IllegalArgumentException {
    public RejectionReasonRequiredException() {
        super("rejectionReason is required when rejectedQuantity is greater than zero");
    }
}
