package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.model.PurchaseOrderSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Allocates the backend Purchase Order identifier atomically.
 * Format: PO-001, PO-002, PO-003 ...
 */
@Service
public class PurchaseOrderNumberGenerator {

    private static final String SEQUENCE_ID = "PO";
    private static final String NUMBER_PREFIX = "PO-";
    private static final int NUMBER_WIDTH = 3;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public PurchaseOrderNumberGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String nextNumber() {
        Query query = Query.query(Criteria.where("_id").is(SEQUENCE_ID));
        Update update = new Update().inc("sequence", 1L);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);

        PurchaseOrderSequence sequence = mongoTemplate.findAndModify(
                query, update, options, PurchaseOrderSequence.class);

        if (sequence == null) {
            throw new IllegalStateException("Unable to allocate purchase order number");
        }

        return NUMBER_PREFIX + String.format("%0" + NUMBER_WIDTH + "d", sequence.getSequence());
    }
}
