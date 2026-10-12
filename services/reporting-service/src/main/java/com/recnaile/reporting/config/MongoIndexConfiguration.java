package com.recnaile.reporting.config;

import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;

@Configuration
public class MongoIndexConfiguration {
 @Bean ApplicationRunner reportingIndexes(MongoTemplate mongo){return args->{
   mongo.indexOps(ReportingFact.class).ensureIndex(new Index().on("eventType",Sort.Direction.ASC).on("occurredAt",Sort.Direction.DESC));
   mongo.indexOps(ReportingFact.class).ensureIndex(new Index().on("entityType",Sort.Direction.ASC).on("entityId",Sort.Direction.ASC).on("occurredAt",Sort.Direction.DESC));
   mongo.indexOps(ReportingProjection.class).ensureIndex(new Index().on("entityType",Sort.Direction.ASC).on("status",Sort.Direction.ASC).on("updatedAt",Sort.Direction.DESC));
   mongo.indexOps(ReportingProjection.class).ensureIndex(new Index().on("departmentId",Sort.Direction.ASC).on("projectId",Sort.Direction.ASC).on("updatedAt",Sort.Direction.DESC));
 };}
}
