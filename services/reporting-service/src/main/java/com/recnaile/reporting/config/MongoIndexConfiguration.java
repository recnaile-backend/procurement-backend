package com.recnaile.reporting.config;

import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import com.recnaile.reporting.model.ReportingSnapshot;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MongoIndexConfiguration {
    @Bean
    ApplicationRunner reportingIndexes(MongoTemplate mongo) {
        return args -> {
            mongo.indexOps(ReportingFact.class).createIndex(
                new Index().on("eventType", Sort.Direction.ASC).on("occurredAt", Sort.Direction.DESC));
            mongo.indexOps(ReportingFact.class).createIndex(
                new Index().on("entityType", Sort.Direction.ASC).on("entityId", Sort.Direction.ASC)
                    .on("occurredAt", Sort.Direction.DESC));
            mongo.indexOps(ReportingProjection.class).createIndex(
                new Index().on("entityType", Sort.Direction.ASC).on("status", Sort.Direction.ASC)
                    .on("updatedAt", Sort.Direction.DESC));
            mongo.indexOps(ReportingProjection.class).createIndex(
                new Index().on("departmentId", Sort.Direction.ASC).on("projectId", Sort.Direction.ASC)
                    .on("updatedAt", Sort.Direction.DESC));
            mongo.indexOps(ReportingFact.class).createIndex(
                new Index().on("departmentId", Sort.Direction.ASC).on("projectId", Sort.Direction.ASC)
                    .on("occurredAt", Sort.Direction.DESC));
            mongo.indexOps(ReportingSnapshot.class).getIndexInfo().stream()
                .filter(info -> "snapshotDate_1".equals(info.getName()))
                .findFirst().ifPresent(info -> mongo.indexOps(ReportingSnapshot.class).dropIndex(info.getName()));
            mongo.indexOps(ReportingSnapshot.class).createIndex(
                new Index().named("snapshot_date_scope_unique").on("snapshotDate", Sort.Direction.ASC).on("departmentId", Sort.Direction.ASC)
                    .on("projectId", Sort.Direction.ASC).unique());
            mongo.indexOps(ReportingSnapshot.class).createIndex(
                new Index().named("snapshot_scope_date").on("departmentId", Sort.Direction.ASC).on("projectId", Sort.Direction.ASC)
                    .on("snapshotDate", Sort.Direction.DESC));
        };
    }
}
