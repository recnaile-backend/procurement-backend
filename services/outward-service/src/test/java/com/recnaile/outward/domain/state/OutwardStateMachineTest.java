package com.recnaile.outward.domain.state;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.exception.InvalidOutwardTransitionException;
import org.junit.jupiter.api.Test;

class OutwardStateMachineTest {

    @Test
    void pendingApprovalCanBecomeApprovedThenIssued() {
        OutwardRequest request = new OutwardRequest();
        request.setStatus(OutwardStatus.PENDING_APPROVAL.name());

        OutwardStateMachine.transition(request, OutwardStatus.APPROVED, Instant.now());
        assertEquals(OutwardStatus.APPROVED.name(), request.getStatus());

        OutwardStateMachine.transition(request, OutwardStatus.ISSUED, Instant.now());
        assertEquals(OutwardStatus.ISSUED.name(), request.getStatus());
    }

    @Test
    void issuedCannotBeReissued() {
        OutwardRequest request = new OutwardRequest();
        request.setStatus(OutwardStatus.ISSUED.name());

        assertThrows(InvalidOutwardTransitionException.class,
                () -> OutwardStateMachine.transition(request, OutwardStatus.ISSUED, Instant.now()));
    }

    @Test
    void pendingApprovalCanBeRejected() {
        OutwardRequest request = new OutwardRequest();
        request.setStatus(OutwardStatus.PENDING_APPROVAL.name());

        OutwardStateMachine.transition(request, OutwardStatus.REJECTED, Instant.now());
        assertEquals(OutwardStatus.REJECTED.name(), request.getStatus());
    }
}
