package com.recnaile.asset.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import jakarta.annotation.PostConstruct;

/** Upgrades the outwardNumber index from the previous unique design to the production non-unique outward-number design. */
@Configuration
@ConditionalOnProperty(prefix = "app.mongodb", name = "ensure-asset-indexes", havingValue = "true", matchIfMissing = true)
public class AssetMongoIndexInitializer {
    private final MongoTemplate mongoTemplate;

    public AssetMongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void ensureIndexes() {
        migrateLegacyReferenceFields();
        var indexOps = mongoTemplate.indexOps("assets");
        List<String> legacyOrCurrentOutwardIndexes = indexOps.getIndexInfo().stream()
                .filter(info -> info.getIndexFields().stream().anyMatch(field ->
                        "outwardNumber".equals(field.getKey()) || "issueReference".equals(field.getKey())))
                .map(info -> info.getName())
                .toList();
        for (String indexName : legacyOrCurrentOutwardIndexes) {
            indexOps.dropIndex(indexName);
        }
        indexOps.createIndex(new Index().on("outwardNumber", Sort.Direction.ASC).named("outward_number_idx"));

        var assignmentIndexOps = mongoTemplate.indexOps("asset_assignments");
        List<String> legacyAssignmentOutwardIndexes = assignmentIndexOps.getIndexInfo().stream()
                .filter(info -> info.getIndexFields().stream().anyMatch(field -> "outwardId".equals(field.getKey())))
                .map(info -> info.getName())
                .toList();
        for (String indexName : legacyAssignmentOutwardIndexes) {
            assignmentIndexOps.dropIndex(indexName);
        }
    }

    private void migrateLegacyReferenceFields() {
        mongoTemplate.updateMulti(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("issueReference").exists(true)
                                .and("outwardNumber").exists(false)),
                new org.springframework.data.mongodb.core.query.Update().rename("issueReference", "outwardNumber"),
                "assets");

        mongoTemplate.updateMulti(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("outwardId").exists(true)
                                .and("outwardNumber").exists(false)),
                new org.springframework.data.mongodb.core.query.Update().rename("outwardId", "outwardNumber"),
                "asset_assignments");
    }
}
