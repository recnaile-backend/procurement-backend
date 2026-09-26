package com.recnaile.inward.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.inward.model.OutboxEvent;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    Optional<OutboxEvent> findByEventId(String eventId);
    List<OutboxEvent> findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List<OutboxEvent.Status> statuses, Instant now);
}
