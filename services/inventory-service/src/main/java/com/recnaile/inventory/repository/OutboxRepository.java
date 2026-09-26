package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.OutboxEventDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutboxRepository extends MongoRepository<OutboxEventDocument,String> {
    List<OutboxEventDocument> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
