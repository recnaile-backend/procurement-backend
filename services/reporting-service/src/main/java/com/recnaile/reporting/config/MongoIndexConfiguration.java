package com.recnaile.reporting.config;

import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import com.recnaile.reporting.model.ReportingSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit, idempotent indexes for reporting query patterns.
 * Keep this list aligned with PHASE-12-INDEXES-PERFORMANCE.md.
 */
@Configuration
public class MongoIndexConfiguration {
    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfiguration.class);

    @Bean
    ApplicationRunner reportingIndexes(MongoTemplate mongo) {
        return args -> {
            // Immutable event facts: period reports, event-type counts, entity history and freshness.
            ensure(mongo, ReportingFact.class, new Index()
                .named("fact_type_occurred").on("eventType", Sort.Direction.ASC)
                .on("occurredAt", Sort.Direction.DESC));
            ensure(mongo, ReportingFact.class, new Index()
                .named("fact_entity_occurred").on("entityType", Sort.Direction.ASC)
                .on("entityId", Sort.Direction.ASC).on("occurredAt", Sort.Direction.DESC));
            ensure(mongo, ReportingFact.class, new Index()
                .named("fact_ingested_at").on("ingestedAt", Sort.Direction.DESC));
            ensure(mongo, ReportingFact.class, new Index()
                .named("fact_occurred_at").on("occurredAt", Sort.Direction.DESC));
            ensure(mongo, ReportingFact.class, new Index()
                .named("fact_scope_occurred").on("departmentName", Sort.Direction.ASC)
                .on("projectId", Sort.Direction.ASC).on("occurredAt", Sort.Direction.DESC));
            ensure(mongo, ReportingFact.class, new Index()
                .named("fact_type_scope_occurred").on("eventType", Sort.Direction.ASC)
                .on("departmentName", Sort.Direction.ASC).on("projectId", Sort.Direction.ASC)
                .on("occurredAt", Sort.Direction.DESC));

            // Current projections: idempotent entity upserts, scoped dashboard counts and aging queries.
            ensure(mongo, ReportingProjection.class, new Index()
                .named("projection_entity_unique").on("entityType", Sort.Direction.ASC)
                .on("entityId", Sort.Direction.ASC).unique());
            ensure(mongo, ReportingProjection.class, new Index()
                .named("projection_type_status_updated").on("entityType", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC).on("updatedAt", Sort.Direction.DESC));
            ensure(mongo, ReportingProjection.class, new Index()
                .named("projection_department_updated").on("departmentName", Sort.Direction.ASC)
                .on("updatedAt", Sort.Direction.DESC));
            ensure(mongo, ReportingProjection.class, new Index()
                .named("projection_type_scope_updated").on("entityType", Sort.Direction.ASC)
                .on("departmentName", Sort.Direction.ASC).on("projectId", Sort.Direction.ASC)
                .on("updatedAt", Sort.Direction.DESC));
            ensure(mongo, ReportingProjection.class, new Index()
                .named("projection_type_status_scope_updated").on("entityType", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC).on("departmentName", Sort.Direction.ASC)
                .on("projectId", Sort.Direction.ASC).on("updatedAt", Sort.Direction.DESC));

            // Snapshots: one snapshot per UTC date and exact scope; efficient newest-first history.
            // Remove only the known legacy single-field index that conflicts with the scoped key strategy.
            mongo.indexOps(ReportingSnapshot.class).getIndexInfo().stream()
                .filter(info -> "snapshotDate_1".equals(info.getName()))
                .findFirst().ifPresent(info -> {
                    log.info("Removing legacy reporting snapshot index {}", info.getName());
                    mongo.indexOps(ReportingSnapshot.class).dropIndex(info.getName());
                });
            ensure(mongo, ReportingSnapshot.class, new Index()
                .named("snapshot_date_scope_unique").on("snapshotDate", Sort.Direction.ASC)
                .on("departmentName", Sort.Direction.ASC).on("projectId", Sort.Direction.ASC).unique());
            ensure(mongo, ReportingSnapshot.class, new Index()
                .named("snapshot_scope_date").on("departmentName", Sort.Direction.ASC)
                .on("projectId", Sort.Direction.ASC).on("snapshotDate", Sort.Direction.DESC));

            log.info("Reporting indexes ensured for facts, projections and snapshots");
        };
    }

    private void ensure(MongoTemplate mongo, Class<?> type, Index index) {
        Document desiredKeys = index.getIndexKeys();
        Document desiredOptions = index.getIndexOptions();
        String collectionName = mongo.getCollectionName(type);

        // If an explicitly managed index keeps its name but its key fields changed
        // (for example departmentId -> departmentName), MongoDB raises IndexKeySpecsConflict.
        // Replace only that named index; do not drop collections or unrelated indexes.
        String desiredName = desiredOptions.getString("name");
        if (desiredName != null) {
            for (Document existing : mongo.getCollection(collectionName).listIndexes(Document.class)) {
                if (desiredName.equals(existing.getString("name"))) {
                    Document existingKeys = existing.get("key", Document.class);
                    if (!sameOrderedKeys(existingKeys, desiredKeys)) {
                        log.warn("Replacing outdated reporting index {} on {} because its key fields changed",
                            desiredName, collectionName);
                        mongo.indexOps(type).dropIndex(desiredName);
                    }
                    break;
                }
            }
        }

        // MongoDB may already have an equivalent index under a generated/different name.
        // Reuse it rather than creating a duplicate index.
        for (Document existing : mongo.getCollection(collectionName).listIndexes(Document.class)) {
            Document existingKeys = existing.get("key", Document.class);
            if (sameOrderedKeys(existingKeys, desiredKeys)) {
                ensureCompatibleOptions(existing, desiredOptions, type, collectionName);
                log.info("Reusing existing equivalent index {} on {}", existing.getString("name"), collectionName);
                return;
            }
        }

        mongo.indexOps(type).createIndex(index);
        log.debug("Created reporting index {} on {}", desiredKeys, type.getSimpleName());
    }

    private boolean sameOrderedKeys(Document existing, Document desired) {
        if (existing == null || desired == null || existing.size() != desired.size()) return false;
        List<Map.Entry<String, Object>> existingEntries = new ArrayList<>(existing.entrySet());
        List<Map.Entry<String, Object>> desiredEntries = new ArrayList<>(desired.entrySet());
        for (int i = 0; i < existingEntries.size(); i++) {
            Map.Entry<String, Object> left = existingEntries.get(i);
            Map.Entry<String, Object> right = desiredEntries.get(i);
            if (!left.getKey().equals(right.getKey()) || !Objects.equals(left.getValue(), right.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void ensureCompatibleOptions(Document existing, Document desired, Class<?> type, String collectionName) {
        // Index names can differ while the key pattern is identical. Options that change
        // query or uniqueness semantics must still match; never silently weaken a unique index.
        for (String option : List.of("unique", "sparse", "expireAfterSeconds", "partialFilterExpression", "collation")) {
            Object existingValue = existing.get(option);
            Object desiredValue = desired.get(option);
            if (!Objects.equals(existingValue, desiredValue)) {
                throw new IllegalStateException("Existing MongoDB index " + existing.getString("name")
                    + " on " + collectionName + " has the same key pattern as the configured index for "
                    + type.getSimpleName() + " but incompatible '" + option + "' option. "
                    + "Review the existing index before applying a migration.");
            }
        }
    }
}
