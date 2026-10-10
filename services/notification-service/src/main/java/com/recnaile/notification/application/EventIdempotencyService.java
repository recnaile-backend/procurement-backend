package com.recnaile.notification.application;

import com.recnaile.notification.domain.model.EventProcessingRecord;
import com.recnaile.notification.persistence.repository.EventProcessingRecordRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/** Stores event processing state; event IDs are unique across the service. */
@Service
public class EventIdempotencyService {
    private final EventProcessingRecordRepository repository;

    public EventIdempotencyService(EventProcessingRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** Returns true only when this call creates the initial processing record. */
    public boolean registerIfNew(String eventId, String eventType, Instant now) {
        if (repository.existsByEventId(eventId)) return false;
        try {
            repository.save(new EventProcessingRecord(eventId, eventType, now));
            return true;
        } catch (DuplicateKeyException duplicate) {
            // A concurrent consumer instance registered the same event first.
            if (repository.existsByEventId(eventId)) return false;
            throw duplicate;
        }
    }

    public EventProcessingRecord markProcessed(String eventId, Instant now) {
        EventProcessingRecord record = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("No processing record for eventId"));
        record.markProcessed(now);
        return repository.save(record);
    }

    public EventProcessingRecord markFailed(String eventId, String safeFailureCode, Instant now) {
        EventProcessingRecord record = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("No processing record for eventId"));
        record.markFailed(safeFailureCode, now);
        return repository.save(record);
    }
}
