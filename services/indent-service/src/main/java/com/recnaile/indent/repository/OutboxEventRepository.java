package com.recnaile.indent.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.recnaile.indent.domain.model.OutboxEvent;
import com.recnaile.indent.domain.model.OutboxEvent.OutboxStatus;

@Repository
public interface OutboxEventRepository
        extends MongoRepository<OutboxEvent, String> {

    Optional<OutboxEvent> findByEventId(String eventId);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
            OutboxStatus status,
            Pageable pageable);

    /*
     * Returns the oldest event for an aggregate that has not yet
     * reached PUBLISHED state.
     *
     * This is the ordering gate used by OutboxPublisher.
     *
     * Therefore:
     *
     * PENDING event #1
     * PENDING event #2
     *
     * #2 cannot publish until #1 becomes PUBLISHED.
     *
     * A FAILED event also blocks later events until the failed event
     * is resolved.
     */
    Optional<OutboxEvent>
    findFirstByAggregateIdAndStatusNotOrderByCreatedAtAscIdAsc(
            String aggregateId,
            OutboxStatus status);

    long countByStatus(OutboxStatus status);

    long deleteByStatusAndPublishedAtBefore(
            OutboxStatus status,
            Instant publishedAtBefore);
}