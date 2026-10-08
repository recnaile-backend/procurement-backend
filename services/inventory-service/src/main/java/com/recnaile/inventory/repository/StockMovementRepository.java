package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.StockMovementDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StockMovementRepository extends MongoRepository<StockMovementDocument, String> {
    Optional<StockMovementDocument> findByEventId(String eventId);

    List<StockMovementDocument> findByItemIdOrderByOccurredAtDesc(String itemId);
    List<StockMovementDocument> findByItemId(String itemId);

    List<StockMovementDocument> findBySourceTransactionIdAndItemIdAndEventType(
            String sourceTransactionId, String itemId, String eventType);
}
