package com.recnaile.outward.outbox;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {

    Optional<OutboxEvent> findByEventId(String eventId);

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
