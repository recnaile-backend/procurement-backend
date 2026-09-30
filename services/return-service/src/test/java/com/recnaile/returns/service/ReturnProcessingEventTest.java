package com.recnaile.returns.service;

import com.recnaile.returns.event.ReturnEventPublisher;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnProcessingEventTest {

    @Mock
    private ReturnRequestRepository repository;

    @Mock
    private ReturnEventPublisher eventPublisher;

    @Test
    void publishesEventOnlyAfterSuccessfulPersistence() {
        Instant now = Instant.parse("2026-09-30T06:00:00Z");
        ReturnRequest request = new ReturnRequest(
                "OUT-1", "LINE-1", "ITEM-1", new BigDecimal("2"),
                "USER-1", ReturnCondition.GOOD, "ok", null,
                Instant.parse("2026-09-30T05:00:00Z"));
        request.setReturnId("RET-1");
        request.setStatus(ReturnStatus.VERIFIED);

        when(repository.findById("RET-1")).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);

        ReturnProcessingService service = new ReturnProcessingService(
                repository,
                new ReturnStateMachine(),
                Clock.fixed(now, ZoneOffset.UTC),
                eventPublisher);

        service.process("RET-1");

        verify(repository).save(request);
        verify(eventPublisher).publish(request);
    }
}
