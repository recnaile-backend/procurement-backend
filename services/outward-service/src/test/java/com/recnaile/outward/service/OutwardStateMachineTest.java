package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recnaile.outward.domain.enums.OutwardStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OutwardStateMachineTest {

    private OutwardStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OutwardStateMachine();
    }

    @Test
    void allowsDraftToPendingApproval() {
        assertEquals(OutwardStatus.PENDING_APPROVAL,
                stateMachine.transition(OutwardStatus.DRAFT, OutwardStatus.PENDING_APPROVAL));
    }

    @Test
    void allowsPendingApprovalToApproved() {
        assertEquals(OutwardStatus.APPROVED,
                stateMachine.transition(OutwardStatus.PENDING_APPROVAL, OutwardStatus.APPROVED));
    }

    @Test
    void allowsPendingApprovalToRejected() {
        assertEquals(OutwardStatus.REJECTED,
                stateMachine.transition(OutwardStatus.PENDING_APPROVAL, OutwardStatus.REJECTED));
    }

    @Test
    void allowsApprovedToIssued() {
        assertEquals(OutwardStatus.ISSUED,
                stateMachine.transition(OutwardStatus.APPROVED, OutwardStatus.ISSUED));
    }

    @Test
    void rejectsSkippingApproval() {
        InvalidOutwardStateTransitionException ex = assertThrows(
                InvalidOutwardStateTransitionException.class,
                () -> stateMachine.transition(OutwardStatus.DRAFT, OutwardStatus.APPROVED));
        assertEquals(InvalidOutwardStateTransitionException.ERROR_CODE, ex.getErrorCode());
    }

    @Test
    void rejectsSkippingIssueApproval() {
        assertThrows(InvalidOutwardStateTransitionException.class,
                () -> stateMachine.transition(OutwardStatus.PENDING_APPROVAL, OutwardStatus.ISSUED));
    }

    @Test
    void rejectsTransitionsFromTerminalStates() {
        for (OutwardStatus terminal : new OutwardStatus[] {
                OutwardStatus.REJECTED, OutwardStatus.ISSUED, OutwardStatus.CANCELLED }) {
            assertTrue(stateMachine.allowedTransitions(terminal).isEmpty());
            assertThrows(InvalidOutwardStateTransitionException.class,
                    () -> stateMachine.transition(terminal, OutwardStatus.DRAFT));
        }
    }

    @Test
    void rejectsNullStatuses() {
        assertFalse(stateMachine.canTransition(null, OutwardStatus.DRAFT));
        assertFalse(stateMachine.canTransition(OutwardStatus.DRAFT, null));
        assertThrows(InvalidOutwardStateTransitionException.class,
                () -> stateMachine.transition(null, OutwardStatus.DRAFT));
        assertThrows(InvalidOutwardStateTransitionException.class,
                () -> stateMachine.transition(OutwardStatus.DRAFT, null));
    }

    @Test
    void doesNotExposeMutableTransitionSet() {
        assertThrows(UnsupportedOperationException.class,
                () -> stateMachine.allowedTransitions(OutwardStatus.DRAFT).clear());
    }
}
