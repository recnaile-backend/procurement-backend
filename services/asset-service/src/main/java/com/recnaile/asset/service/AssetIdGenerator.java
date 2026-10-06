package com.recnaile.asset.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.recnaile.asset.model.AssetId;

/**
 * Generates stable asset IDs in the format ASSET%2FYY-YY%2FNNN.
 * The sequence is atomic per financial year and is persisted in MongoDB.
 */
@Component
public class AssetIdGenerator {

    private static final String COUNTER_COLLECTION = "asset_id_sequences";
    private static final String SEQUENCE_FIELD = "sequence";

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public AssetIdGenerator(MongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public AssetId next() {
        LocalDate date = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        int startYear = date.getMonthValue() >= Month.APRIL.getValue()
                ? date.getYear()
                : date.getYear() - 1;
        int endYear = startYear + 1;

        String financialYear = String.format("%02d-%02d", startYear % 100, endYear % 100);
        String counterId = "ASSET/" + financialYear;

        Document counter = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(counterId)),
                new Update().inc(SEQUENCE_FIELD, 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Document.class,
                COUNTER_COLLECTION);

        if (counter == null || counter.get(SEQUENCE_FIELD) == null) {
            throw new IllegalStateException("Unable to generate asset ID sequence for " + financialYear);
        }

        long sequence = ((Number) counter.get(SEQUENCE_FIELD)).longValue();
        if (sequence > 999_999) {
            throw new IllegalStateException("Asset ID sequence exceeded supported range for " + financialYear);
        }

        return new AssetId(String.format("ASSET%%2F%s%%2F%03d", financialYear, sequence));
    }
}
