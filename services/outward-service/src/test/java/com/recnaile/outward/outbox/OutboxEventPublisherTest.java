package com.recnaile.outward.outbox;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class OutboxEventPublisherTest {

    @Test
    void publishMarksEventPublishedAfterKafkaAcknowledgement() throws Exception {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        OutboxEventPublisher publisher = new OutboxEventPublisher(repository, kafka, "outward.events", 1000, 3,
                Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC));

        OutboxEvent event = event();
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("outward.events", 0), 5, 1, 0L, 0, 0);
        SendResult<String, String> result = new SendResult<>(null, metadata);
        when(kafka.send("outward.events", "OUT%2F26-27%2F001", "{\"eventType\":\"OUTWARD_ISSUED\"}"))
                .thenReturn(CompletableFuture.completedFuture(result));

        publisher.publish(event);

        verify(kafka).send("outward.events", "OUT%2F26-27%2F001", event.getPayload());
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(1, event.getAttemptCount());
    }

    @Test
    void publishKeepsPendingWhenKafkaFailsBeforeMaxAttempts() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        OutboxEventPublisher publisher = new OutboxEventPublisher(repository, kafka, "outward.events", 1000, 3, Clock.systemUTC());
        OutboxEvent event = event();
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(failed);

        publisher.publish(event);

        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertTrue(event.getLastError().contains("broker unavailable"));
    }

    private OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setEventId("event-1");
        event.setEventType("OUTWARD_ISSUED");
        event.setAggregateType("OUTWARD_REQUEST");
        event.setAggregateId("OUT%2F26-27%2F001");
        event.setPayload("{\"eventType\":\"OUTWARD_ISSUED\"}");
        event.setStatus(OutboxEventStatus.PENDING);
        event.setCreatedAt(Instant.now());
        event.setAttemptCount(0);
        return event;
    }
}
