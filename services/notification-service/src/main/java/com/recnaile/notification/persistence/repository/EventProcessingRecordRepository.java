package com.recnaile.notification.persistence.repository;

import com.recnaile.notification.domain.model.EventProcessingRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EventProcessingRecordRepository extends MongoRepository<EventProcessingRecord, String> {
    Optional<EventProcessingRecord> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
