package com.recnaile.purchaseorder.repository;

import com.recnaile.purchaseorder.domain.model.IdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByActorIdAndKey(String actorId, String key);
}
