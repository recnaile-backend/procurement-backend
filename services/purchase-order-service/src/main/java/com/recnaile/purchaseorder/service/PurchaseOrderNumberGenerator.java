package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.model.PurchaseOrderSequence;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Allocates PO numbers atomically in MongoDB.
 *
 * Format mandated by the project specification: PO-YYYY-0001.
 * The sequence is scoped to the calendar year in the PO number.
 */
@Service
public class PurchaseOrderNumberGenerator {

    private static final String SEQUENCE_PREFIX = "PO";
    private static final String NUMBER_PREFIX = "PO-";
    private static final int NUMBER_WIDTH = 4;

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public PurchaseOrderNumberGenerator(MongoTemplate mongoTemplate) {
        this(mongoTemplate, Clock.systemUTC());
    }

    public PurchaseOrderNumberGenerator(MongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public String nextNumber() {
        return nextNumber(LocalDate.now(clock));
    }

    public String nextNumber(LocalDate poDate) {
        if (poDate == null) {
            throw new IllegalArgumentException("PO date is required to generate a PO number");
        }

        String year = Integer.toString(poDate.getYear());
        String sequenceId = SEQUENCE_PREFIX + ":" + year;

        Query query = Query.query(Criteria.where("_id").is(sequenceId));
        Update update = new Update().inc("sequence", 1L);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);

        PurchaseOrderSequence sequence = mongoTemplate.findAndModify(
                query,
                update,
                options,
                PurchaseOrderSequence.class);

        if (sequence == null) {
            throw new IllegalStateException("Unable to allocate purchase order number for year " + year);
        }

        return NUMBER_PREFIX + year + "-" + String.format("%0" + NUMBER_WIDTH + "d", sequence.getSequence());
    }
}
