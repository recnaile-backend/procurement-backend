package com.recnaile.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class AssetIdGeneratorTest {

    @Test
    void generatesFinancialYearAssetIdWithThreeDigitSequence() {
        MongoTemplate mongoTemplate = org.mockito.Mockito.mock(MongoTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC);
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Document.class), eq("asset_id_sequences")))
                .thenReturn(new Document("_id", "ASSET:26-27").append("sequence", 1L));

        AssetIdGenerator generator = new AssetIdGenerator(mongoTemplate, clock);

        assertEquals("ASSET%2F26-27%2F001", generator.next().value());
    }
}
