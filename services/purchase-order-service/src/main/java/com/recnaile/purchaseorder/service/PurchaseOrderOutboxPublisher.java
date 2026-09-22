package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.enums.OutboxStatus;
import com.recnaile.purchaseorder.domain.model.OutboxEvent;
import com.recnaile.purchaseorder.repository.OutboxEventRepository;
import com.recnaile.purchaseorder.observability.PurchaseOrderMetrics;
import com.recnaile.purchaseorder.observability.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PurchaseOrderOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderOutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final long initialBackoffSeconds;
    private final long maxBackoffSeconds;
    private final PurchaseOrderMetrics metrics;

    @Autowired
    public PurchaseOrderOutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.initial-backoff-seconds:2}") long initialBackoffSeconds,
            @Value("${app.outbox.max-backoff-seconds:300}") long maxBackoffSeconds,
            PurchaseOrderMetrics metrics) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.initialBackoffSeconds = initialBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
        this.metrics = metrics;
    }

    public PurchaseOrderOutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            int batchSize,
            long initialBackoffSeconds,
            long maxBackoffSeconds) {
        this(repository, kafkaTemplate, batchSize, initialBackoffSeconds, maxBackoffSeconds, null);
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize));

        for (OutboxEvent event : events) {
            if (!isEligible(event)) {
                continue;
            }

            if (!isNextForAggregate(event)) {
                continue;
            }

            publish(event);
        }
    }

    private boolean isEligible(OutboxEvent event) {
        return event.getNextAttemptAt() == null || !event.getNextAttemptAt().isAfter(Instant.now());
    }

    private boolean isNextForAggregate(OutboxEvent event) {
        return repository.findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc(
                        event.getAggregateId(), OutboxStatus.PUBLISHED)
                .map(first -> first.getId().equals(event.getId()))
                .orElse(true);
    }

    private void publish(OutboxEvent event) {
        Instant attemptAt = Instant.now();
        CorrelationIdFilter.putEventId(event.getEventId());
        event.setAttemptCount(event.getAttemptCount() + 1);
        event.setLastAttemptAt(attemptAt);

        try {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .get(120, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setNextAttemptAt(null);
            event.setLastError(null);
            event.setUpdatedAt(Instant.now());
            repository.save(event);
            if (metrics != null) {
                metrics.recordOutboxPublished(event.getEventType());
            }

            log.info(
                    "Outbox event published successfully: eventId={}, eventType={}, aggregateId={}, topic={}",
                    event.getEventId(), event.getEventType(), event.getAggregateId(), event.getTopic());
        } catch (Exception ex) {
            event.setStatus(OutboxStatus.PENDING);
            event.setNextAttemptAt(nextAttemptAt(event.getAttemptCount()));
            event.setLastError(trimError(ex));
            event.setUpdatedAt(Instant.now());
            repository.save(event);
            if (metrics != null) {
                metrics.recordOutboxFailure(event.getEventType());
            }

            log.error(
                    "Failed to publish outbox event: eventId={}, eventType={}, attemptCount={}, nextAttemptAt={}, status={}",
                    event.getEventId(), event.getEventType(), event.getAttemptCount(),
                    event.getNextAttemptAt(), event.getStatus(), ex);
        } finally {
            CorrelationIdFilter.putEventId(null);
        }
    }

    private Instant nextAttemptAt(int attemptCount) {
        long multiplier;
        if (attemptCount >= 31) {
            multiplier = Long.MAX_VALUE;
        } else {
            multiplier = 1L << Math.max(0, attemptCount - 1);
        }

        long seconds;
        try {
            seconds = Math.multiplyExact(initialBackoffSeconds, multiplier);
        } catch (ArithmeticException ex) {
            seconds = maxBackoffSeconds;
        }

        seconds = Math.min(seconds, maxBackoffSeconds);
        return Instant.now().plus(Duration.ofSeconds(seconds));
    }

    private String trimError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
