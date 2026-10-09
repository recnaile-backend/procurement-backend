package com.recnaile.returns.service;

import com.recnaile.returns.exception.InvalidReturnStateTransitionException;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import com.recnaile.returns.repository.ReturnRequestRepository;
import com.recnaile.returns.outbox.ReturnEventOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private ReturnEventOutboxService outboxService;

    @Test
    void processGoodReturnSetsAcceptedGood() {
        Instant now = Instant.parse("2026-09-30T06:00:00Z");
        ReturnRequest request = returnRequest(ReturnCondition.GOOD);
        request.setStatus(ReturnStatus.VERIFIED);
        when(repository.findByReturnId("RETURN%2F26-27%2F001")).thenReturn(Optional.of(request));
        when(repository.save(request)).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.fixed(now, ZoneOffset.UTC), outboxService);

        ReturnRequest result = service.process(" RETURN%2F26-27%2F001 ");

        assertThat(result.getStatus()).isEqualTo(ReturnStatus.ACCEPTED_GOOD);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        verify(repository).save(request);
        org.mockito.Mockito.verify(outboxService).enqueue(request);
    }

    @Test
    void processDamagedReturnSetsAcceptedDamaged() {
        ReturnRequest request = returnRequest(ReturnCondition.DAMAGED);
        request.setStatus(ReturnStatus.VERIFIED);
        when(repository.findByReturnId("RETURN%2F26-27%2F001")).thenReturn(Optional.of(request));
        when(repository.save(request)).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC(), outboxService);

        ReturnRequest result = service.process("RETURN%2F26-27%2F001");

        assertThat(result.getStatus()).isEqualTo(ReturnStatus.ACCEPTED_DAMAGED);
        verify(repository).save(request);
        org.mockito.Mockito.verify(outboxService).enqueue(request);
    }

    @Test
    void processRejectsUnknownReturn() {
        when(repository.findByReturnId("RETURN%2F26-27%2F404")).thenReturn(Optional.empty());
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC(), outboxService);

        assertThatThrownBy(() -> service.process("RETURN%2F26-27%2F404"))
                .isInstanceOf(ReturnNotFoundException.class);
    }

    @Test
    void processRejectsPendingReturn() {
        ReturnRequest request = returnRequest(ReturnCondition.GOOD);
        when(repository.findByReturnId("RETURN%2F26-27%2F001")).thenReturn(Optional.of(request));
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC(), outboxService);

        assertThatThrownBy(() -> service.process("RETURN%2F26-27%2F001"))
                .isInstanceOf(InvalidReturnStateTransitionException.class);
    }

    @Test
    void processRejectsAlreadyProcessedReturn() {
        ReturnRequest request = returnRequest(ReturnCondition.GOOD);
        request.setStatus(ReturnStatus.ACCEPTED_GOOD);
        when(repository.findByReturnId("RETURN%2F26-27%2F001")).thenReturn(Optional.of(request));
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC(), outboxService);

        assertThatThrownBy(() -> service.process("RETURN%2F26-27%2F001"))
                .isInstanceOf(InvalidReturnStateTransitionException.class);
    }

    @Test
    void processRejectsBlankReturnId() {
        ReturnProcessingService service = new ReturnProcessingService(
                repository, new ReturnStateMachine(), Clock.systemUTC(), outboxService);

        assertThatThrownBy(() -> service.process("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("returnId must not be blank");
    }

    private ReturnRequest returnRequest(ReturnCondition condition) {
        return new ReturnRequest(
                "OUT-100", "ITEM-10", 2,
                "user-1", condition, "return",
                Instant.parse("2026-09-30T05:00:00Z"));
    }
}
