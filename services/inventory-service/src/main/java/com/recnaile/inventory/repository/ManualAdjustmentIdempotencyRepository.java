package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.ManualAdjustmentIdempotencyDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ManualAdjustmentIdempotencyRepository extends MongoRepository<ManualAdjustmentIdempotencyDocument, String> {
    Optional<ManualAdjustmentIdempotencyDocument> findByIdempotencyKey(String idempotencyKey);
}
