package com.recnaile.asset.outbox;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetOutboxRepository extends MongoRepository<AssetOutboxEvent, String> {
    Optional<AssetOutboxEvent> findFirstByStatusOrderByCreatedAtAsc(AssetOutboxEvent.Status status);

    Optional<AssetOutboxEvent> findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(AssetOutboxEvent.Status status, Instant now);

    Optional<AssetOutboxEvent> findFirstByStatusAndLockedUntilLessThanEqualOrderByCreatedAtAsc(AssetOutboxEvent.Status status, Instant now);
}
