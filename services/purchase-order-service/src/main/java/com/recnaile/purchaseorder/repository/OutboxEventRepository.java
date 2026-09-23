package com.recnaile.purchaseorder.repository;

import com.recnaile.purchaseorder.domain.enums.OutboxStatus;
import com.recnaile.purchaseorder.domain.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    Optional<OutboxEvent> findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc(
            String aggregateId,
            OutboxStatus status);
}
