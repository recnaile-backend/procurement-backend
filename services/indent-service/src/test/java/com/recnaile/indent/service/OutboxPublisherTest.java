package com.recnaile.indent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.domain.model.OutboxEvent;
import com.recnaile.indent.domain.model.OutboxEvent.OutboxStatus;
import com.recnaile.indent.repository.OutboxEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private SendResult<String, String> sendResult;

    @Mock
    private RecordMetadata recordMetadata;

    private OutboxPublisher publisher;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {

        meterRegistry =
                new SimpleMeterRegistry();

        publisher =
                new OutboxPublisher(
                        outboxEventRepository,
                        kafkaTemplate,
                        new ObjectMapper(),
                        meterRegistry);
    }

    @Test
    void publishPendingEvents_shouldPublishAndMarkEventAsPublished()
            throws Exception {

        OutboxEvent event =
                pendingEvent();

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(List.of(event));

        mockAsAggregateHead(event);

        when(
                kafkaTemplate.send(
                        eq("procurement.indent.events"),
                        eq("IND-001"),
                        anyString()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                sendResult));

        when(sendResult.getRecordMetadata())
                .thenReturn(recordMetadata);

        when(recordMetadata.partition())
                .thenReturn(0);

        when(recordMetadata.offset())
                .thenReturn(10L);

        publisher.publishPendingEvents();

        ArgumentCaptor<String> envelopeCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(
                eq("procurement.indent.events"),
                eq("IND-001"),
                envelopeCaptor.capture());

        JsonNode envelope =
                new ObjectMapper()
                        .readTree(
                                envelopeCaptor.getValue());

        assertThat(
                envelope.get("eventId").asText())
                .isEqualTo(
                        event.getEventId());

        assertThat(
                envelope.get("eventType").asText())
                .isEqualTo(
                        "INDENT_SUBMITTED");

        assertThat(
                envelope.get("eventVersion").asInt())
                .isEqualTo(1);

        assertThat(
                envelope.get("producer").asText())
                .isEqualTo(
                        "indent-service");

        assertThat(
                envelope.get("correlationId").asText())
                .isEqualTo(
                        event.getCorrelationId());

        assertThat(
                envelope.get("traceId").asText())
                .isEqualTo(
                        event.getTraceId());

        assertThat(
                envelope.get("payload")
                        .get("indentId")
                        .asText())
                .isEqualTo(
                        "IND-001");

        verify(
                outboxEventRepository)
                .save(event);

        assertThat(event.getStatus())
                .isEqualTo(
                        OutboxStatus.PUBLISHED);

        assertThat(event.getPublishedAt())
                .isNotNull();

        assertThat(event.getLastAttemptAt())
                .isNotNull();

        assertThat(event.getAttemptCount())
                .isEqualTo(1);

        assertThat(event.getLastError())
                .isNull();

        assertThat(event.getNextAttemptAt())
                .isNull();

        assertThat(
                meterRegistry
                        .get(
                                "outbox_publish_failures_total")
                        .counter()
                        .count())
                .isZero();

        assertThat(
                meterRegistry
                        .get(
                                "outbox_publish_latency")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void publishPendingEvents_shouldKeepEventPendingWhenKafkaFails() {

        OutboxEvent event =
                pendingEvent();

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(List.of(event));

        mockAsAggregateHead(event);

        CompletableFuture<SendResult<String, String>>
                failedFuture =
                        new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException(
                        "Kafka unavailable"));

        when(
                kafkaTemplate.send(
                        eq("procurement.indent.events"),
                        eq("IND-001"),
                        anyString()))
                .thenReturn(failedFuture);

        publisher.publishPendingEvents();

        verify(kafkaTemplate).send(
                eq("procurement.indent.events"),
                eq("IND-001"),
                anyString());

        verify(
                outboxEventRepository)
                .save(event);

        assertThat(event.getStatus())
                .isEqualTo(
                        OutboxStatus.PENDING);

        assertThat(event.getPublishedAt())
                .isNull();

        assertThat(event.getLastAttemptAt())
                .isNotNull();

        assertThat(event.getAttemptCount())
                .isEqualTo(1);

        assertThat(event.getLastError())
                .contains(
                        "Kafka unavailable");

        assertThat(event.getNextAttemptAt())
                .isNotNull()
                .isAfter(Instant.now());

        assertThat(
                meterRegistry
                        .get(
                                "outbox_publish_failures_total")
                        .counter()
                        .count())
                .isEqualTo(1);

        assertThat(
                meterRegistry
                        .get(
                                "outbox_publish_latency")
                        .timer()
                        .count())
                .isZero();
    }

    @Test
    void publishPendingEvents_shouldUseExponentialBackoffForRetries() {

        ReflectionTestUtils.setField(
                publisher,
                "retryJitterRatio",
                0.0d);

        OutboxEvent event =
                pendingEvent();

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(List.of(event));

        mockAsAggregateHead(event);

        CompletableFuture<SendResult<String, String>>
                failedFuture =
                        new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException(
                        "Kafka unavailable"));

        when(
                kafkaTemplate.send(
                        eq("procurement.indent.events"),
                        eq("IND-001"),
                        anyString()))
                .thenReturn(failedFuture);

        publisher.publishPendingEvents();

        Instant firstRetryAt =
                event.getNextAttemptAt();

        event.setNextAttemptAt(
                Instant.now());

        publisher.publishPendingEvents();

        Instant secondRetryAt =
                event.getNextAttemptAt();

        assertThat(firstRetryAt)
                .isNotNull();

        assertThat(secondRetryAt)
                .isNotNull();

        assertThat(secondRetryAt)
                .isAfter(firstRetryAt);

        assertThat(event.getAttemptCount())
                .isEqualTo(2);
    }

    @Test
    void pendingMetric_shouldReflectRepositoryPendingCount() {

        when(
                outboxEventRepository
                        .countByStatus(
                                OutboxStatus.PENDING))
                .thenReturn(7L);

        assertThat(
                meterRegistry
                        .get(
                                "outbox_pending_count")
                        .gauge()
                        .value())
                .isEqualTo(7.0d);
    }

    @Test
    void publishPendingEvents_shouldSkipEventUntilBackoffExpires() {

        OutboxEvent event =
                pendingEvent();

        event.setNextAttemptAt(
                Instant.now()
                        .plusSeconds(30));

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(List.of(event));

        publisher.publishPendingEvents();

        verify(
                kafkaTemplate,
                never())
                .send(
                        anyString(),
                        anyString(),
                        anyString());

        verify(
                outboxEventRepository,
                never())
                .save(
                        any(OutboxEvent.class));
    }

    @Test
    void publishPendingEvents_shouldDoNothingWhenThereAreNoPendingEvents() {

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(
                kafkaTemplate,
                never())
                .send(
                        anyString(),
                        anyString(),
                        anyString());

        verify(
                outboxEventRepository,
                never())
                .save(
                        any(OutboxEvent.class));
    }

    @Test
    void publishPendingEvents_shouldPublishMultipleEvents() {

        OutboxEvent first =
                pendingEvent("IND-001");

        OutboxEvent second =
                pendingEvent("IND-002");

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(
                        List.of(first, second));

        mockAsAggregateHead(first);
        mockAsAggregateHead(second);

        when(
                kafkaTemplate.send(
                        anyString(),
                        anyString(),
                        anyString()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                sendResult));

        when(sendResult.getRecordMetadata())
                .thenReturn(recordMetadata);

        when(recordMetadata.partition())
                .thenReturn(0);

        when(recordMetadata.offset())
                .thenReturn(1L);

        publisher.publishPendingEvents();

        verify(kafkaTemplate)
                .send(
                        eq("procurement.indent.events"),
                        eq("IND-001"),
                        anyString());

        verify(kafkaTemplate)
                .send(
                        eq("procurement.indent.events"),
                        eq("IND-002"),
                        anyString());

        verify(
                outboxEventRepository,
                times(2))
                .save(
                        any(OutboxEvent.class));

        assertThat(first.getStatus())
                .isEqualTo(
                        OutboxStatus.PUBLISHED);

        assertThat(second.getStatus())
                .isEqualTo(
                        OutboxStatus.PUBLISHED);
    }

    @Test
    void publishPendingEvents_shouldNotPublishLaterEventWhenEarlierEventIsPending() {

        OutboxEvent first =
                pendingEvent("IND-001");

        OutboxEvent second =
                pendingEvent("IND-001");

        first.setId("OUTBOX-001");
        first.setEventId("EVENT-001");

        second.setId("OUTBOX-002");
        second.setEventId("EVENT-002");

        first.setCreatedAt(
                Instant.parse(
                        "2026-09-15T10:00:00Z"));

        second.setCreatedAt(
                Instant.parse(
                        "2026-09-15T10:01:00Z"));

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(
                        List.of(second));

        when(
                outboxEventRepository
                        .findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc(
                                eq("IND-001"),
                                eq(OutboxStatus.PUBLISHED)))
                .thenReturn(
                        Optional.of(first));

        publisher.publishPendingEvents();

        verify(
                kafkaTemplate,
                never())
                .send(
                        anyString(),
                        anyString(),
                        anyString());

        verify(
                outboxEventRepository,
                never())
                .save(second);
    }

    @Test
    void publishPendingEvents_shouldBlockLaterEventWhenEarlierEventFailed() {

        OutboxEvent failed =
                pendingEvent("IND-001");

        OutboxEvent later =
                pendingEvent("IND-001");

        failed.setId("OUTBOX-001");
        failed.setEventId("EVENT-001");
        failed.setStatus(
                OutboxStatus.FAILED);

        later.setId("OUTBOX-002");
        later.setEventId("EVENT-002");

        failed.setCreatedAt(
                Instant.parse(
                        "2026-09-15T10:00:00Z"));

        later.setCreatedAt(
                Instant.parse(
                        "2026-09-15T10:01:00Z"));

        when(
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                eq(OutboxStatus.PENDING),
                                any(Pageable.class)))
                .thenReturn(
                        List.of(later));

        when(
                outboxEventRepository
                        .findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc(
                                eq("IND-001"),
                                eq(OutboxStatus.PUBLISHED)))
                .thenReturn(
                        Optional.of(failed));

        publisher.publishPendingEvents();

        verify(
                kafkaTemplate,
                never())
                .send(
                        anyString(),
                        anyString(),
                        anyString());

        verify(
                outboxEventRepository,
                never())
                .save(later);
    }

    private void mockAsAggregateHead(
            OutboxEvent event) {

        when(
                outboxEventRepository
                        .findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc(
                                eq(event.getAggregateId()),
                                eq(OutboxStatus.PUBLISHED)))
                .thenReturn(
                        Optional.of(event));
    }

    private OutboxEvent pendingEvent() {

        return pendingEvent("IND-001");
    }

    private OutboxEvent pendingEvent(
            String indentId) {

        Instant now =
                Instant.now();

        return OutboxEvent.builder()
                .id(
                        "OUTBOX-" + indentId)
                .eventId(
                        "EVENT-" + indentId)
                .eventType(
                        "INDENT_SUBMITTED")
                .eventVersion("1")
                .occurredAt(now)
                .producerService(
                        "indent-service")
                .aggregateType("INDENT")
                .aggregateId(indentId)
                .actorId(
                        "employee-001")
                .actorRole(
                        "EMPLOYEE")
                .topic(
                        "procurement.indent.events")
                .payload(
                        "{\"indentId\":\""
                                + indentId
                                + "\"}")
                .correlationId(
                        "00000000-0000-0000-0000-000000000001")
                .traceId(
                        "00000000000000000000000000000001")
                .status(
                        OutboxStatus.PENDING)
                .attemptCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}