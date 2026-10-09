package com.recnaile.inventory.config;

import com.recnaile.inventory.domain.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MongoIndexConfig {
    @Bean
    CommandLineRunner inventoryIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            mongoTemplate.indexOps(StockBalanceDocument.class)
                    .createIndex(new Index().on("balanceKey", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(StockBalanceDocument.class)
                    .createIndex(new Index().on("itemId", Sort.Direction.ASC)
                            .on("departmentName", Sort.Direction.ASC)
                            .on("projectId", Sort.Direction.ASC));

            // A new deterministic movement key gives duplicate Kafka deliveries
            // an atomic database guard without requiring destructive migration of
            // older movement documents that predate this field.
            mongoTemplate.indexOps(StockMovementDocument.class).getIndexInfo().stream()
                    .filter(index -> index.getName().equals("uk_stock_movement_event_item_type"))
                    .forEach(index -> mongoTemplate.indexOps(StockMovementDocument.class).dropIndex(index.getName()));
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .createIndex(new Index().on("eventMovementKey", Sort.Direction.ASC).unique().sparse());
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .createIndex(new Index().on("eventId", Sort.Direction.ASC));
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .createIndex(new Index().on("itemId", Sort.Direction.ASC)
                            .on("occurredAt", Sort.Direction.DESC));
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .createIndex(new Index().on("sourceTransactionId", Sort.Direction.ASC)
                            .on("itemId", Sort.Direction.ASC)
                            .on("eventType", Sort.Direction.ASC));

            var idempotencyIndexes = mongoTemplate.indexOps(IdempotencyDocument.class).getIndexInfo();
            idempotencyIndexes.stream()
                    .filter(index -> index.getIndexFields().size() == 1
                            && index.getIndexFields().get(0).getKey().equals("eventId")
                            && !index.isUnique())
                    .forEach(index -> mongoTemplate.indexOps(IdempotencyDocument.class).dropIndex(index.getName()));
            mongoTemplate.indexOps(IdempotencyDocument.class)
                    .createIndex(new Index().on("eventId", Sort.Direction.ASC).unique());

            mongoTemplate.indexOps(StockAllocationDocument.class)
                    .createIndex(new Index().on("outwardId", Sort.Direction.ASC)
                            .on("itemId", Sort.Direction.ASC).unique());

            mongoTemplate.indexOps(ManualAdjustmentIdempotencyDocument.class)
                    .createIndex(new Index().on("idempotencyKey", Sort.Direction.ASC).unique());

            mongoTemplate.indexOps(OutboxEventDocument.class)
                    .createIndex(new Index().on("published", Sort.Direction.ASC)
                            .on("processing", Sort.Direction.ASC)
                            .on("createdAt", Sort.Direction.ASC));
            mongoTemplate.indexOps(InventoryAuditDocument.class)
                    .createIndex(new Index().on("entityId", Sort.Direction.ASC)
                            .on("createdAt", Sort.Direction.DESC));
        };
    }
}
