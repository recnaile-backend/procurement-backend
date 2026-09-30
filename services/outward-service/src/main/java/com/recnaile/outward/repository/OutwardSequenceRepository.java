package com.recnaile.outward.repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class OutwardSequenceRepository {

    private final MongoTemplate mongoTemplate;

    public OutwardSequenceRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public long nextSequence(String fiscalYearKey) {
        Query query = Query.query(Criteria.where("_id").is(fiscalYearKey));
        Update update = new Update().inc("sequence", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);
        OutwardSequenceDocument document = mongoTemplate.findAndModify(
                query, update, options, OutwardSequenceDocument.class);
        if (document == null) {
            throw new IllegalStateException("Unable to allocate outward number sequence");
        }
        return document.getSequence();
    }
}
