package com.recnaile.inward.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.inward.model.IdempotencyRecord;

public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByScopeKey(String scopeKey);
}
