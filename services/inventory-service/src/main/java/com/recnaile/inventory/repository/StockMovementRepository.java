package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.StockMovementDocument;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StockMovementRepository extends MongoRepository<StockMovementDocument, String> {
    Optional<StockMovementDocument> findByEventId(String eventId);

    Page<StockMovementDocument> findByItemIdOrderByOccurredAtDesc(String itemId, Pageable pageable);
}
