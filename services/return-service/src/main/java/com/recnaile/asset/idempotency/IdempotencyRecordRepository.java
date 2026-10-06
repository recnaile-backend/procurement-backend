package com.recnaile.asset.idempotency;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByOperationAndIdempotencyKey(String operation, String idempotencyKey);
}
