package com.recnaile.indent.service;

import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.exception.InvalidIndentTransitionException;
import com.recnaile.indent.domain.state.IndentStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndentStateMachineTest {

    @Test
    void validTransitions_shouldBeAccepted() {
        assertThat(IndentStateMachine.canTransition(
                IndentStatus.DRAFT, IndentStatus.SUBMITTED)).isTrue();
        assertThat(IndentStateMachine.canTransition(
                IndentStatus.SUBMITTED, IndentStatus.UNDER_VERIFICATION)).isTrue();
        assertThat(IndentStateMachine.canTransition(
                IndentStatus.UNDER_VERIFICATION, IndentStatus.VERIFIED)).isTrue();
        assertThat(IndentStateMachine.canTransition(
                IndentStatus.VERIFIED, IndentStatus.PENDING_APPROVAL)).isTrue();
        assertThat(IndentStateMachine.canTransition(
                IndentStatus.PENDING_APPROVAL, IndentStatus.APPROVED)).isTrue();
        assertThat(IndentStateMachine.canTransition(
                IndentStatus.APPROVED, IndentStatus.READY_FOR_PO)).isTrue();
    }

    @Test
    void invalidTransition_shouldBeRejected() {
        assertThat(IndentStateMachine.canTransition(
                IndentStatus.SUBMITTED, IndentStatus.APPROVED)).isFalse();

        assertThatThrownBy(() -> IndentStateMachine.validateTransition(
                IndentStatus.READY_FOR_PO, IndentStatus.APPROVED))
                .isInstanceOf(InvalidIndentTransitionException.class);
    }
}
