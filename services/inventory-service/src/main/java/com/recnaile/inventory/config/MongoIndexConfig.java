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
                    .ensureIndex(new Index().on("balanceKey", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(StockBalanceDocument.class)
                    .ensureIndex(new Index().on("itemId", Sort.Direction.ASC)
                            .on("departmentName", Sort.Direction.ASC)
                            .on("projectId", Sort.Direction.ASC));
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .ensureIndex(new Index().on("eventId", Sort.Direction.ASC));
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .ensureIndex(new Index().on("itemId", Sort.Direction.ASC)
                            .on("occurredAt", Sort.Direction.DESC));
            var idempotencyIndexes = mongoTemplate.indexOps(IdempotencyDocument.class).getIndexInfo();
            idempotencyIndexes.stream()
                    .filter(index -> index.getIndexFields().size() == 1
                            && index.getIndexFields().get(0).getKey().equals("eventId")
                            && !index.isUnique())
                    .forEach(index -> mongoTemplate.indexOps(IdempotencyDocument.class).dropIndex(index.getName()));
            mongoTemplate.indexOps(IdempotencyDocument.class)
                    .ensureIndex(new Index().on("eventId", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(OutboxEventDocument.class)
                    .ensureIndex(new Index().on("published", Sort.Direction.ASC)
                            .on("createdAt", Sort.Direction.ASC));
            mongoTemplate.indexOps(InventoryAuditDocument.class)
                    .ensureIndex(new Index().on("entityId", Sort.Direction.ASC)
                            .on("createdAt", Sort.Direction.DESC));
        };
    }
}
