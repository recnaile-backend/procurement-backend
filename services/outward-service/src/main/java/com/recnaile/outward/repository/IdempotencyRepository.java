package com.recnaile.outward.repository;

import java.util.Optional;

import com.recnaile.outward.domain.model.IdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdempotencyRepository extends MongoRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByScopeKey(String scopeKey);
}
