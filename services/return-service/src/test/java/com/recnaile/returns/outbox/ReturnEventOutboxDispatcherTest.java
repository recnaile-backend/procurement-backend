package com.recnaile.returns.outbox;

import com.recnaile.returns.event.KafkaReturnEventPublisher;
import com.recnaile.returns.model.enums.OutboxStatus;
import com.recnaile.returns.model.enums.ReturnCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnEventOutboxDispatcherTest {

    @Mock
    private ReturnEventOutboxRepository repository;

    @Mock
    private KafkaReturnEventPublisher publisher;

    @Test
    void successfulPublishMarksOutboxSent() {
        Instant created = Instant.parse("2026-09-30T06:00:00Z");
        Instant now = Instant.parse("2026-09-30T06:05:00Z");
        ReturnEventOutbox outbox = new ReturnEventOutbox(
                "event-1", "RET-1", "RETURN_RECORDED", "return.recorded",
                "RET-1", "ITEM-1", 2, ReturnCondition.GOOD,
                created, created);
        when(publisher.publish(eq("return.recorded"), eq("RET-1"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        ReturnEventOutboxDispatcher dispatcher = new ReturnEventOutboxDispatcher(
                repository, publisher, Clock.fixed(now, ZoneOffset.UTC));
        dispatcher.dispatchOne(outbox);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(outbox.getPublishedAt()).isEqualTo(now);
        verify(repository).save(outbox);
    }

    @Test
    void failedPublishKeepsOutboxPendingAndRecordsError() {
        Instant created = Instant.parse("2026-09-30T06:00:00Z");
        ReturnEventOutbox outbox = new ReturnEventOutbox(
                "event-2", "RET-2", "RETURN_RECORDED", "return.recorded",
                "RET-2", "ITEM-2", 1, ReturnCondition.DAMAGED,
                created, created);
        when(publisher.publish(eq("return.recorded"), eq("RET-2"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        ReturnEventOutboxDispatcher dispatcher = new ReturnEventOutboxDispatcher(
                repository, publisher, Clock.fixed(created, ZoneOffset.UTC));
        dispatcher.dispatchOne(outbox);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getLastError()).contains("Kafka unavailable");
        verify(repository).save(outbox);
    }
}
