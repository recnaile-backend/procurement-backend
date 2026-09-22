package com.recnaile.indent.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.recnaile.indent.domain.model.OutboxEvent.OutboxStatus;
import com.recnaile.indent.repository.OutboxEventRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OutboxRetentionService {

    private static final Duration DEFAULT_RETENTION = Duration.ofDays(7);

    private final OutboxEventRepository outboxEventRepository;

    @Value("${app.outbox.published-retention:7d}")
    private Duration publishedRetention;

    public OutboxRetentionService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Deletes successfully published outbox events after the configured
     * retention period. FAILED events are deliberately retained.
     */
    @Scheduled(fixedDelayString = "${app.outbox.retention-check-interval:3600000}")
    public void purgePublishedEvents() {
        Instant cutoff = Instant.now().minus(effectiveRetention());

        long deleted = outboxEventRepository.deleteByStatusAndPublishedAtBefore(
                OutboxStatus.PUBLISHED, cutoff);

        if (deleted > 0) {
            log.info("Purged published outbox events: deleted={}, cutoff={}", deleted, cutoff);
        }
    }

    private Duration effectiveRetention() {
        if (publishedRetention == null
                || publishedRetention.isNegative()
                || publishedRetention.isZero()) {
            return DEFAULT_RETENTION;
        }
        return publishedRetention;
    }
}
