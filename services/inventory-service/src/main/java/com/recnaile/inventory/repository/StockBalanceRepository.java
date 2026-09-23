package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.StockBalanceDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StockBalanceRepository extends MongoRepository<StockBalanceDocument,String> {
    Optional<StockBalanceDocument> findByBalanceKey(String balanceKey);
    List<StockBalanceDocument> findByItemId(String itemId);
}
