package com.recnaile.returns.service;

import com.recnaile.returns.exception.InvalidReturnStateTransitionException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReturnStateMachine {

    public ReturnRequest verify(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("returnRequest must not be null");
        }
        return transition(returnRequest, ReturnStatus.VERIFIED);
    }

    /**
     * Processes a verified return according to its recorded physical condition.
     * This transition records the authoritative Return Service state only;
     * inventory/asset side effects are handled by later integration phases.
     */
    public ReturnRequest process(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("returnRequest must not be null");
        }
        ReturnCondition condition = returnRequest.getCondition();
        if (condition == null) {
            throw new IllegalArgumentException("returnRequest condition must not be null");
        }
        ReturnStatus target = condition == ReturnCondition.GOOD
                ? ReturnStatus.ACCEPTED_GOOD
                : ReturnStatus.ACCEPTED_DAMAGED;
        return transition(returnRequest, target);
    }

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
        returnRequest.setUpdatedAt(Instant.now());
        return returnRequest;
    }

    public boolean isAllowed(ReturnStatus currentStatus, ReturnStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        return (currentStatus == ReturnStatus.PENDING_VERIFICATION
                    && targetStatus == ReturnStatus.VERIFIED)
                || (currentStatus == ReturnStatus.VERIFIED
                    && (targetStatus == ReturnStatus.ACCEPTED_GOOD
                        || targetStatus == ReturnStatus.ACCEPTED_DAMAGED));
    }
}
