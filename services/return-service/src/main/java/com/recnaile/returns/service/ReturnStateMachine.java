package com.recnaile.returns.service;

import com.recnaile.returns.exception.InvalidReturnStateTransitionException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.springframework.stereotype.Service;

/**
 * Application-layer state machine for the Return lifecycle.
 *
 * The documented Return workflow establishes a verification boundary:
 * PENDING_VERIFICATION -> VERIFIED. No other persisted transition is allowed
 * by this phase. Good/Damaged processing is intentionally handled in a later
 * business-processing phase.
 */
@Service
public class ReturnStateMachine {

    /**
     * Verifies a return and moves it from PENDING_VERIFICATION to VERIFIED.
     *
     * @param returnRequest return aggregate to transition
     * @return the same aggregate after the valid transition
     * @throws IllegalArgumentException when the aggregate or current status is invalid
     * @throws InvalidReturnStateTransitionException when verification is not allowed
     */
    public ReturnRequest verify(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("returnRequest must not be null");
        }

        return transition(returnRequest, ReturnStatus.VERIFIED);
    }

    /**
     * Applies an explicitly requested lifecycle transition after validating the
     * documented transition graph.
     */
    public ReturnRequest transition(ReturnRequest returnRequest, ReturnStatus targetStatus) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("returnRequest must not be null");
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("targetStatus must not be null");
        }

        ReturnStatus currentStatus = returnRequest.getStatus();
        if (currentStatus == null) {
            throw new IllegalArgumentException("returnRequest status must not be null");
        }

        if (!isAllowed(currentStatus, targetStatus)) {
            throw new InvalidReturnStateTransitionException(currentStatus, targetStatus);
        }

        returnRequest.setStatus(targetStatus);
        returnRequest.setUpdatedAt(java.time.Instant.now());
        return returnRequest;
    }

    /**
     * Returns whether a lifecycle transition is allowed by the current state machine.
     */
    public boolean isAllowed(ReturnStatus currentStatus, ReturnStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        return currentStatus == ReturnStatus.PENDING_VERIFICATION
                && targetStatus == ReturnStatus.VERIFIED;
    }
}
