package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.enums.OutboxStatus;
import com.recnaile.purchaseorder.domain.model.OutboxEvent;
import com.recnaile.purchaseorder.repository.OutboxEventRepository;
import com.recnaile.purchaseorder.service.PurchaseOrderOutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseOrderOutboxPublisherTest {

    private OutboxEventRepository repository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private PurchaseOrderOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new PurchaseOrderOutboxPublisher(repository, kafkaTemplate, 50, 2, 300);
    }

    @Test
    void publishPendingEvents_shouldPublishAndMarkPublished() {
        OutboxEvent event = pendingEvent("PO-001", "EVENT-001", Instant.parse("2026-09-19T09:00:00Z"));

        when(repository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(repository.findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc("PO-001", OutboxStatus.PUBLISHED))
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send("procurement.purchase-order.events", "PO-001", event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        verify(kafkaTemplate).send("procurement.purchase-order.events", "PO-001", event.getPayload());
        verify(repository).save(event);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void publishPendingEvents_shouldKeepEventPendingWhenKafkaFails() {
        OutboxEvent event = pendingEvent("PO-001", "EVENT-001", Instant.parse("2026-09-19T09:00:00Z"));

        when(repository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(repository.findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc("PO-001", OutboxStatus.PUBLISHED))
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send("procurement.purchase-order.events", "PO-001", event.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        publisher.publishPendingEvents();

        verify(repository).save(event);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(event.getLastError()).contains("Kafka unavailable");
    }

    @Test
    void publishPendingEvents_shouldPreserveAggregateOrder() {
        OutboxEvent first = pendingEvent("PO-001", "EVENT-001", Instant.parse("2026-09-19T09:00:00Z"));
        OutboxEvent second = pendingEvent("PO-001", "EVENT-002", Instant.parse("2026-09-19T09:01:00Z"));

        when(repository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(second));
        when(repository.findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc("PO-001", OutboxStatus.PUBLISHED))
                .thenReturn(Optional.of(first));

        publisher.publishPendingEvents();

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(repository, never()).save(second);
    }

    private OutboxEvent pendingEvent(String aggregateId, String eventId, Instant createdAt) {
        OutboxEvent event = new OutboxEvent();
        event.setId("OUTBOX-" + eventId);
        event.setEventId(eventId);
        event.setEventType("PO_CREATED");
        event.setEventVersion("1");
        event.setOccurredAt(createdAt);
        event.setProducerService("purchase-order-service");
        event.setAggregateType("PURCHASE_ORDER");
        event.setAggregateId(aggregateId);
        event.setTopic("procurement.purchase-order.events");
        event.setPayload("{\"eventType\":\"PO_CREATED\"}");
        event.setStatus(OutboxStatus.PENDING);
        event.setAttemptCount(0);
        event.setCreatedAt(createdAt);
        event.setUpdatedAt(createdAt);
        return event;
    }
}
