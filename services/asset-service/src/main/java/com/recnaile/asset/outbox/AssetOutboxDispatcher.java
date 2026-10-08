package com.recnaile.asset.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Retries durable outbox records until Kafka acknowledges publication. */
@Component
public class AssetOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(AssetOutboxDispatcher.class);
    private final AssetOutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    public AssetOutboxDispatcher(AssetOutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate, Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox-dispatch-interval-ms:1000}")
    public void dispatch() {
        for (int i = 0; i < 50; i++) {
            AssetOutboxEvent event = claimNext();
            if (event == null) return;
            kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                    .whenComplete((result, error) -> {
                        AssetOutboxEvent current = repository.findById(event.getEventId()).orElse(null);
                        if (current == null) return;
                        if (error == null) {
                            current.markPublished(Instant.now(clock));
                        } else {
                            Duration backoff = Duration.ofSeconds(Math.min(300, 1L << Math.min(current.getAttempts(), 8)));
                            current.markFailed(error.getMessage(), Instant.now(clock).plus(backoff));
                            log.error("Failed to publish Asset outbox event {}", event.getEventId(), error);
                        }
                        repository.save(current);
                    });
        }
    }

    private AssetOutboxEvent claimNext() {
        Instant now = Instant.now(clock);
        AssetOutboxEvent candidate = repository.findFirstByStatusOrderByCreatedAtAsc(AssetOutboxEvent.Status.PENDING).orElse(null);
        if (candidate == null) {
            candidate = repository.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                    AssetOutboxEvent.Status.FAILED, now).orElse(null);
        }
        if (candidate == null) {
            candidate = repository.findFirstByStatusAndLockedUntilLessThanEqualOrderByCreatedAtAsc(
                    AssetOutboxEvent.Status.PUBLISHING, now).orElse(null);
            if (candidate != null) {
                candidate.markFailed("Recovered expired outbox publication lease", now);
            }
        }
        if (candidate == null) return null;
        candidate.markPublishing(now.plusSeconds(60));
        return repository.save(candidate);
    }
}
