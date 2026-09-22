package com.recnaile.indent.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.recnaile.indent.domain.model.OutboxEvent;
import com.recnaile.indent.domain.model.OutboxEvent.OutboxStatus;
import com.recnaile.indent.repository.OutboxEventRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private static final int DEFAULT_MAX_ATTEMPTS = 10;

    private static final Duration DEFAULT_RETRY_INITIAL_DELAY =
            Duration.ofSeconds(1);

    private static final Duration DEFAULT_RETRY_MAX_DELAY =
            Duration.ofMinutes(1);

    private static final double DEFAULT_RETRY_JITTER_RATIO = 0.20d;

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    private final Counter publishFailures;

    private final Timer publishLatency;

    @Value("${app.outbox.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.outbox.retry-initial-delay:1s}")
    private Duration retryInitialDelay;

    @Value("${app.outbox.retry-max-delay:1m}")
    private Duration retryMaxDelay;

    @Value("${app.outbox.retry-jitter-ratio:0.20}")
    private double retryJitterRatio;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;

        Gauge.builder(
                        "outbox_pending_count",
                        outboxEventRepository,
                        repository ->
                                repository.countByStatus(
                                        OutboxStatus.PENDING))
                .description("Number of pending outbox events")
                .register(meterRegistry);

        this.publishFailures =
                Counter.builder(
                                "outbox_publish_failures_total")
                        .description(
                                "Number of failed outbox publish attempts")
                        .register(meterRegistry);

        this.publishLatency =
                Timer.builder(
                                "outbox_publish_latency")
                        .description(
                                "Time from outbox event creation to successful publication")
                        .publishPercentileHistogram()
                        .register(meterRegistry);

        /*
         * Defaults are useful for direct unit-test construction where
         * Spring @Value injection does not execute.
         */
        this.retryInitialDelay =
                DEFAULT_RETRY_INITIAL_DELAY;

        this.retryMaxDelay =
                DEFAULT_RETRY_MAX_DELAY;

        this.retryJitterRatio =
                DEFAULT_RETRY_JITTER_RATIO;
    }

    /**
     * Poll unpublished outbox events and publish them to Kafka.
     *
     * Ordering guarantees:
     *
     * 1. aggregateId is used as the Kafka partition key.
     * 2. Only the oldest unresolved event for an aggregate can publish.
     * 3. A later event cannot overtake a PENDING event.
     * 4. A later event cannot overtake a FAILED event.
     * 5. A retrying event therefore blocks later events for the same
     *    aggregate until it succeeds or is operator-resolved.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.outbox.publisher-interval:1000}")
    public void publishPendingEvents() {

        Instant now = Instant.now();

        List<OutboxEvent> events =
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                OutboxStatus.PENDING,
                                PageRequest.of(
                                        0,
                                        BATCH_SIZE));

        for (OutboxEvent event : events) {

            if (!isEligibleForAttempt(event, now)) {
                continue;
            }

            if (!isAggregateHead(event)) {
                log.debug(
                        "Skipping outbox event because an earlier unresolved event exists for aggregate: eventId={}, aggregateId={}",
                        event.getEventId(),
                        event.getAggregateId());

                continue;
            }

            publish(event);
        }
    }

    /**
     * Determines whether this event is the oldest unresolved event
     * for its aggregate.
     *
     * Only PUBLISHED events are ignored by the ordering query.
     *
     * This means a FAILED event intentionally blocks later events
     * until an operator resolves it.
     */
    private boolean isAggregateHead(OutboxEvent event) {

        if (event.getAggregateId() == null
                || event.getAggregateId().isBlank()) {

            /*
             * An outbox event without an aggregate ID cannot participate
             * in aggregate ordering. Existing contracts require an
             * aggregate ID, so fail closed rather than guessing.
             */
            log.error(
                    "Outbox event has no aggregateId; refusing to publish: eventId={}",
                    event.getEventId());

            return false;
        }

        return outboxEventRepository
                .findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc(
                        event.getAggregateId(),
                        OutboxStatus.PUBLISHED)
                .map(firstUnresolved ->
                        sameEvent(firstUnresolved, event))
                .orElse(false);
    }

    private boolean sameEvent(
            OutboxEvent first,
            OutboxEvent candidate) {

        if (first == candidate) {
            return true;
        }

        if (first.getId() != null
                && candidate.getId() != null) {

            return Objects.equals(
                    first.getId(),
                    candidate.getId());
        }

        return Objects.equals(
                first.getEventId(),
                candidate.getEventId());
    }

    private boolean isEligibleForAttempt(
            OutboxEvent event,
            Instant now) {

        return event.getNextAttemptAt() == null
                || !event.getNextAttemptAt()
                        .isAfter(now);
    }

    private void publish(OutboxEvent event) {

        Instant attemptTime = Instant.now();

        try {

            event.setLastAttemptAt(attemptTime);

            event.setAttemptCount(
                    event.getAttemptCount() + 1);

            /*
             * aggregateId is deliberately used as the Kafka key.
             *
             * Kafka guarantees ordering for records sharing the same
             * partition, and the aggregate ID therefore keeps all
             * events for one indent on the same partition.
             */
            SendResult<String, String> result =
                    kafkaTemplate
                            .send(
                                    event.getTopic(),
                                    event.getAggregateId(),
                                    buildEnvelope(event))
                            .get();

            Instant publishedAt = Instant.now();

            event.setStatus(
                    OutboxStatus.PUBLISHED);

            event.setPublishedAt(
                    publishedAt);

            event.setUpdatedAt(
                    publishedAt);

            event.setNextAttemptAt(
                    null);

            event.setLastError(
                    null);

            outboxEventRepository.save(event);

            recordPublishLatency(
                    event,
                    publishedAt);

            log.info(
                    "Outbox event published successfully: eventId={}, eventType={}, aggregateId={}, topic={}, partition={}, offset={}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getTopic(),
                    result.getRecordMetadata()
                            .partition(),
                    result.getRecordMetadata()
                            .offset());

        } catch (Exception ex) {

            Instant failureTime = Instant.now();

            event.setLastAttemptAt(
                    attemptTime);

            event.setUpdatedAt(
                    failureTime);

            event.setLastError(
                    safeErrorMessage(ex));

            publishFailures.increment();

            int attempts =
                    event.getAttemptCount();

            if (attempts >= effectiveMaxAttempts()) {

                event.setStatus(
                        OutboxStatus.FAILED);

                event.setNextAttemptAt(
                        null);

            } else {

                event.setStatus(
                        OutboxStatus.PENDING);

                event.setNextAttemptAt(
                        failureTime.plus(
                                calculateBackoff(
                                        attempts)));
            }

            outboxEventRepository.save(event);

            log.error(
                    "Failed to publish outbox event: eventId={}, eventType={}, aggregateId={}, attemptCount={}, nextAttemptAt={}, status={}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getAttemptCount(),
                    event.getNextAttemptAt(),
                    event.getStatus(),
                    ex);
        }
    }

    private Duration calculateBackoff(
            int attemptNumber) {

        Duration initial =
                effectiveRetryInitialDelay();

        Duration maximum =
                effectiveRetryMaxDelay();

        if (maximum.isZero()
                || maximum.isNegative()) {

            return Duration.ZERO;
        }

        int exponent =
                Math.max(
                        0,
                        Math.min(
                                attemptNumber - 1,
                                30));

        long multiplier;

        try {

            multiplier =
                    1L << exponent;

        } catch (ArithmeticException ex) {

            multiplier =
                    Long.MAX_VALUE;
        }

        Duration base;

        try {

            base =
                    initial.multipliedBy(
                            multiplier);

        } catch (ArithmeticException ex) {

            base =
                    maximum;
        }

        if (base.compareTo(maximum) > 0) {
            base = maximum;
        }

        double jitter =
                Math.max(
                        0.0d,
                        Math.min(
                                retryJitterRatio,
                                1.0d));

        if (jitter == 0.0d) {
            return base;
        }

        double factor =
                1.0d
                        + ThreadLocalRandom.current()
                                .nextDouble(
                                        -jitter,
                                        jitter);

        long jitteredMillis =
                Math.max(
                        0L,
                        Math.round(
                                base.toMillis()
                                        * factor));

        return Duration.ofMillis(
                Math.min(
                        jitteredMillis,
                        maximum.toMillis()));
    }

    private void recordPublishLatency(
            OutboxEvent event,
            Instant publishedAt) {

        Instant createdAt =
                event.getCreatedAt();

        if (createdAt == null) {
            return;
        }

        Duration latency =
                Duration.between(
                        createdAt,
                        publishedAt);

        if (!latency.isNegative()) {
            publishLatency.record(latency);
        }
    }

    private String buildEnvelope(
            OutboxEvent event) {

        try {

            JsonNode payload =
                    objectMapper.readTree(
                            event.getPayload());

            var envelope =
                    objectMapper.createObjectNode();

            envelope.put(
                    "eventId",
                    event.getEventId());

            envelope.put(
                    "eventType",
                    event.getEventType());

            envelope.put(
                    "eventVersion",
                    Integer.parseInt(
                            event.getEventVersion()));

            envelope.put(
                    "occurredAt",
                    event.getOccurredAt()
                            .toString());

            envelope.put(
                    "producer",
                    event.getProducerService());

            envelope.put(
                    "correlationId",
                    event.getCorrelationId());

            envelope.put(
                    "traceId",
                    event.getTraceId());

            envelope.set(
                    "payload",
                    payload);

            return objectMapper.writeValueAsString(
                    envelope);

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to build event envelope for outbox event "
                            + event.getEventId(),
                    ex);
        }
    }

    private int effectiveMaxAttempts() {

        return maxAttempts > 0
                ? maxAttempts
                : DEFAULT_MAX_ATTEMPTS;
    }

    private Duration effectiveRetryInitialDelay() {

        return retryInitialDelay == null
                || retryInitialDelay.isNegative()
                        ? DEFAULT_RETRY_INITIAL_DELAY
                        : retryInitialDelay;
    }

    private Duration effectiveRetryMaxDelay() {

        Duration value =
                retryMaxDelay == null
                        || retryMaxDelay.isNegative()
                                ? DEFAULT_RETRY_MAX_DELAY
                                : retryMaxDelay;

        Duration initial =
                effectiveRetryInitialDelay();

        return value.compareTo(initial) < 0
                ? initial
                : value;
    }

    private String safeErrorMessage(
            Exception ex) {

        String message =
                ex.getMessage();

        if (message == null
                || message.isBlank()) {

            return ex.getClass()
                    .getSimpleName();
        }

        return message.length() > 1000
                ? message.substring(
                        0,
                        1000)
                : message;
    }
}