package com.recnaile.returns.service;

import com.recnaile.returns.exception.InvalidReturnStateTransitionException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReturnStateMachineTest {

    private final ReturnStateMachine stateMachine = new ReturnStateMachine();

    @Test
    void allowsPendingVerificationToVerified() {
        ReturnRequest request = pendingReturn();
        Instant before = request.getUpdatedAt();

        stateMachine.verify(request);

        assertEquals(ReturnStatus.VERIFIED, request.getStatus());
        assertNotNull(request.getUpdatedAt());
        assertFalse(request.getUpdatedAt().isBefore(before));
    }

    @Test
    void rejectsVerifiedToVerifiedAsDuplicateTransition() {
        ReturnRequest request = pendingReturn();
        stateMachine.verify(request);

        InvalidReturnStateTransitionException exception = assertThrows(
                InvalidReturnStateTransitionException.class,
                () -> stateMachine.transition(request, ReturnStatus.VERIFIED));

        assertEquals(ReturnStatus.VERIFIED, exception.getCurrentStatus());
        assertEquals(ReturnStatus.VERIFIED, exception.getRequestedStatus());
    }

    @Test
    void rejectsVerifiedToPendingVerification() {
        ReturnRequest request = pendingReturn();
        stateMachine.verify(request);

        assertThrows(
                InvalidReturnStateTransitionException.class,
                () -> stateMachine.transition(request, ReturnStatus.PENDING_VERIFICATION));
    }

    @Test
    void rejectsNullAggregate() {
        assertThrows(IllegalArgumentException.class, () -> stateMachine.verify(null));
    }

    @Test
    void rejectsNullTargetStatus() {
        ReturnRequest request = pendingReturn();

        assertThrows(
                IllegalArgumentException.class,
                () -> stateMachine.transition(request, null));
    }

    @Test
    void transitionMatrixOnlyAllowsDocumentedTransition() {
        assertTrue(stateMachine.isAllowed(
                ReturnStatus.PENDING_VERIFICATION,
                ReturnStatus.VERIFIED));
        assertFalse(stateMachine.isAllowed(
                ReturnStatus.PENDING_VERIFICATION,
                ReturnStatus.PENDING_VERIFICATION));
        assertFalse(stateMachine.isAllowed(
                ReturnStatus.VERIFIED,
                ReturnStatus.PENDING_VERIFICATION));
        assertFalse(stateMachine.isAllowed(
                ReturnStatus.VERIFIED,
                ReturnStatus.VERIFIED));
    }

    private ReturnRequest pendingReturn() {
        return new ReturnRequest(
                "OUT/26-27/001",
                "OUT-LINE-001",
                "ITEM-001",
                new BigDecimal("2"),
                "EMP-001",
                ReturnCondition.GOOD,
                "Returned in good condition",
                null,
                Instant.parse("2026-09-30T04:00:00Z"));
    }
}
