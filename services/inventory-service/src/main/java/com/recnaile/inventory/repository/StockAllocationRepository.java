package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.StockAllocationDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StockAllocationRepository extends MongoRepository<StockAllocationDocument, String> {
    Optional<StockAllocationDocument> findByOutwardIdAndItemId(String outwardId, String itemId);
}
