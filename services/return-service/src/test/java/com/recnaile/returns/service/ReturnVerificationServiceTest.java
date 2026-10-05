package com.recnaile.returns.service;

import com.recnaile.returns.dto.VerifyReturnRequest;
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
class ReturnVerificationServiceTest {

    @Mock
    private ReturnRequestRepository repository;

    @Test
    void verifyMovesPendingReturnToVerifiedAndRecordsActorAndTimestamp() {
        Instant now = Instant.parse("2026-09-30T05:30:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        ReturnStateMachine stateMachine = new ReturnStateMachine();
        ReturnVerificationService service = new ReturnVerificationService(repository, stateMachine, clock);
        ReturnRequest request = returnRequest();

        when(repository.findById("RET-1")).thenReturn(Optional.of(request));
        when(repository.save(request)).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequest result = service.verify(" RET-1 ", new VerifyReturnRequest(" verifier-1 "));

        assertThat(result.getStatus()).isEqualTo(ReturnStatus.VERIFIED);
        assertThat(result.getVerifiedBy()).isEqualTo("verifier-1");
        assertThat(result.getVerifiedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        verify(repository).save(request);
    }

    @Test
    void verifyRejectsUnknownReturn() {
        ReturnVerificationService service = new ReturnVerificationService(
                repository, new ReturnStateMachine(), Clock.systemUTC());
        when(repository.findById("RET-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("RET-404", new VerifyReturnRequest("verifier-1")))
                .isInstanceOf(ReturnNotFoundException.class);
    }

    @Test
    void verifyRejectsAlreadyVerifiedReturn() {
        ReturnVerificationService service = new ReturnVerificationService(
                repository, new ReturnStateMachine(), Clock.systemUTC());
        ReturnRequest request = returnRequest();
        request.setStatus(ReturnStatus.VERIFIED);
        when(repository.findById("RET-1")).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.verify("RET-1", new VerifyReturnRequest("verifier-1")))
                .isInstanceOf(InvalidReturnStateTransitionException.class);
    }

    @Test
    void verifyRejectsBlankVerifier() {
        ReturnVerificationService service = new ReturnVerificationService(
                repository, new ReturnStateMachine(), Clock.systemUTC());

        assertThatThrownBy(() -> service.verify("RET-1", new VerifyReturnRequest("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("verifiedBy must not be blank");
    }

    private ReturnRequest returnRequest() {
        return new ReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "user-1", ReturnCondition.GOOD, "return", null,
                Instant.parse("2026-09-30T05:00:00Z"));
    }
}
