package com.recnaile.inward;

import com.recnaile.inward.exception.InvalidGRNStateTransitionException;
import com.recnaile.inward.model.GRNStatus;
import com.recnaile.inward.service.GRNStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GRNStateMachineTest {

    private final GRNStateMachine stateMachine = new GRNStateMachine();

    @Test
    void draftCanMoveToReceived() {
        assertTrue(stateMachine.canTransition(
                GRNStatus.DRAFT,
                GRNStatus.RECEIVED
        ));
    }

    @Test
    void receivedCanMoveToPartiallyReceived() {
        assertTrue(stateMachine.canTransition(
                GRNStatus.RECEIVED,
                GRNStatus.PARTIALLY_RECEIVED
        ));
    }

    @Test
    void receivedCanMoveToCompleted() {
        assertTrue(stateMachine.canTransition(
                GRNStatus.RECEIVED,
                GRNStatus.COMPLETED
        ));
    }

    @Test
    void partiallyReceivedCanMoveToCompleted() {
        assertTrue(stateMachine.canTransition(
                GRNStatus.PARTIALLY_RECEIVED,
                GRNStatus.COMPLETED
        ));
    }

    @Test
    void completedIsTerminal() {
        assertTrue(stateMachine.allowedNextStates(GRNStatus.COMPLETED).isEmpty());
    }

    @Test
    void draftCannotSkipToCompleted() {
        assertFalse(stateMachine.canTransition(
                GRNStatus.DRAFT,
                GRNStatus.COMPLETED
        ));
    }

    @Test
    void completedCannotMoveBackToDraft() {
        assertFalse(stateMachine.canTransition(
                GRNStatus.COMPLETED,
                GRNStatus.DRAFT
        ));
    }

    @Test
    void invalidTransitionThrowsDomainException() {
        InvalidGRNStateTransitionException exception =
                assertThrows(
                        InvalidGRNStateTransitionException.class,
                        () -> stateMachine.validateTransition(
                                GRNStatus.DRAFT,
                                GRNStatus.COMPLETED
                        )
                );

        assertEquals(GRNStatus.DRAFT, exception.getCurrentStatus());
        assertEquals(GRNStatus.COMPLETED, exception.getRequestedStatus());
    }
}
