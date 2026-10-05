package com.recnaile.returns.idempotency;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> {
}
