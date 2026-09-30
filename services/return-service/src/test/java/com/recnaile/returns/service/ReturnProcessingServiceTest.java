package com.recnaile.returns.service;

import com.recnaile.returns.exception.InvalidReturnStateTransitionException;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnProcessingServiceTest {

    @Mock
    private ReturnRequestRepository repository;

    @Test
    void processGoodReturnSetsAcceptedGood() {
        Instant now = Instant.parse("2026-09-30T06:00:00Z");
        ReturnRequest request = returnRequest(ReturnCondition.GOOD);
        request.setStatus(ReturnStatus.VERIFIED);
        when(repository.findById("RET-1")).thenReturn(Optional.of(request));
        when(repository.save(request)).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.fixed(now, ZoneOffset.UTC));

        ReturnRequest result = service.process(" RET-1 ");

        assertThat(result.getStatus()).isEqualTo(ReturnStatus.ACCEPTED_GOOD);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        verify(repository).save(request);
    }

    @Test
    void processDamagedReturnSetsAcceptedDamaged() {
        ReturnRequest request = returnRequest(ReturnCondition.DAMAGED);
        request.setStatus(ReturnStatus.VERIFIED);
        when(repository.findById("RET-1")).thenReturn(Optional.of(request));
        when(repository.save(request)).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC());

        ReturnRequest result = service.process("RET-1");

        assertThat(result.getStatus()).isEqualTo(ReturnStatus.ACCEPTED_DAMAGED);
        verify(repository).save(request);
    }

    @Test
    void processRejectsUnknownReturn() {
        when(repository.findById("RET-404")).thenReturn(Optional.empty());
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC());

        assertThatThrownBy(() -> service.process("RET-404"))
                .isInstanceOf(ReturnNotFoundException.class);
    }

    @Test
    void processRejectsPendingReturn() {
        ReturnRequest request = returnRequest(ReturnCondition.GOOD);
        when(repository.findById("RET-1")).thenReturn(Optional.of(request));
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC());

        assertThatThrownBy(() -> service.process("RET-1"))
                .isInstanceOf(InvalidReturnStateTransitionException.class);
    }

    @Test
    void processRejectsAlreadyProcessedReturn() {
        ReturnRequest request = returnRequest(ReturnCondition.GOOD);
        request.setStatus(ReturnStatus.ACCEPTED_GOOD);
        when(repository.findById("RET-1")).thenReturn(Optional.of(request));
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC());

        assertThatThrownBy(() -> service.process("RET-1"))
                .isInstanceOf(InvalidReturnStateTransitionException.class);
    }

    @Test
    void processRejectsBlankReturnId() {
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC());

        assertThatThrownBy(() -> service.process("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("returnId must not be blank");
    }

    private ReturnRequest returnRequest(ReturnCondition condition) {
        return new ReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "user-1", condition, "return", null,
                Instant.parse("2026-09-30T05:00:00Z"));
    }
}
