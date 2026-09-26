package com.recnaile.outward.repository;

import java.time.Instant;
import java.util.List;

import com.recnaile.outward.domain.enums.OutboxStatus;
import com.recnaile.outward.domain.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List<OutboxStatus> statuses, Instant now);
}
