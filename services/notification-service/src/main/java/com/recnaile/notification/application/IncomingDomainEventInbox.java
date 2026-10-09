package com.recnaile.notification.application;

import com.recnaile.notification.domain.model.IncomingDomainEvent;
import com.recnaile.notification.persistence.repository.IncomingDomainEventRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.time.Instant;
import java.util.Objects;

/** Persists the event inbox before Kafka acknowledgment. */
@Service
public class IncomingDomainEventInbox {
    private final IncomingDomainEventRepository repository;

    public IncomingDomainEventInbox(IncomingDomainEventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public IncomingDomainEvent saveIfAbsent(String eventId, String eventType, String topic,
            int partition, long offset, Instant receivedAt, Map<String, Object> envelope) {
        return repository.findByEventId(eventId).orElseGet(() -> saveWithDuplicateProtection(
                eventId, eventType, topic, partition, offset, receivedAt, envelope));
    }

    private IncomingDomainEvent saveWithDuplicateProtection(String eventId, String eventType, String topic,
            int partition, long offset, Instant receivedAt, Map<String, Object> envelope) {
        try {
            return repository.save(new IncomingDomainEvent(eventId, eventType, topic, partition,
                    offset, receivedAt, envelope));
        } catch (DuplicateKeyException duplicate) {
            return repository.findByEventId(eventId).orElseThrow(() -> duplicate);
        }
    }
}
