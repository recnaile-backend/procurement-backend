package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.IdempotencyDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdempotencyRepository extends MongoRepository<IdempotencyDocument,String> {
    boolean existsByEventId(String eventId);
}
