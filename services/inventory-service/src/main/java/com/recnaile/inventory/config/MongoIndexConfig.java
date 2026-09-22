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
                            .on("departmentId", Sort.Direction.ASC)
                            .on("projectId", Sort.Direction.ASC));
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .ensureIndex(new Index().on("eventId", Sort.Direction.ASC));
            mongoTemplate.indexOps(StockMovementDocument.class)
                    .ensureIndex(new Index().on("itemId", Sort.Direction.ASC)
                            .on("occurredAt", Sort.Direction.DESC));
            mongoTemplate.indexOps(IdempotencyDocument.class)
                    .ensureIndex(new Index().on("eventId", Sort.Direction.ASC));
            mongoTemplate.indexOps(OutboxEventDocument.class)
                    .ensureIndex(new Index().on("published", Sort.Direction.ASC)
                            .on("createdAt", Sort.Direction.ASC));
            mongoTemplate.indexOps(InventoryAuditDocument.class)
                    .ensureIndex(new Index().on("entityId", Sort.Direction.ASC)
                            .on("createdAt", Sort.Direction.DESC));
        };
    }
}
