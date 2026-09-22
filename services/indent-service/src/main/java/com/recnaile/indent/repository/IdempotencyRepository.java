package com.recnaile.indent.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.indent.domain.model.IdempotencyRecord;

public interface IdempotencyRepository
        extends MongoRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByActorIdAndOperationAndKey(
            String actorId,
            String operation,
            String key);
}