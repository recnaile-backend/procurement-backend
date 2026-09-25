package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.model.PurchaseOrderSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Allocates the backend Purchase Order identifier atomically.
 *
 * The business identifier is intentionally stored exactly as generated:
 * {@code PO%2F26-27%2F001}. The encoded slash is part of the persisted
 * identifier so it can safely be used as a single URL path-variable value.
 */
@Service
public class PurchaseOrderNumberGenerator {

    private static final String NUMBER_PREFIX = "PO%2F";
    private static final String NUMBER_SEPARATOR = "%2F";
    private static final int NUMBER_WIDTH = 3;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Autowired
    public PurchaseOrderNumberGenerator(MongoTemplate mongoTemplate) {
        this(mongoTemplate, Clock.system(BUSINESS_ZONE));
    }

    public PurchaseOrderNumberGenerator(MongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public String nextNumber() {
        String financialYear = financialYear(LocalDate.now(clock));
        String sequenceId = "PO-" + financialYear;

        Query query = Query.query(Criteria.where("_id").is(sequenceId));
        Update update = new Update().inc("sequence", 1L);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);

        PurchaseOrderSequence sequence = mongoTemplate.findAndModify(
                query, update, options, PurchaseOrderSequence.class);

        if (sequence == null) {
            throw new IllegalStateException("Unable to allocate purchase order number");
        }

        long sequenceNumber = sequence.getSequence();
        while (true) {
            String candidate = NUMBER_PREFIX + financialYear + NUMBER_SEPARATOR
                    + String.format("%0" + NUMBER_WIDTH + "d", sequenceNumber);

            // The sequence collection may be newly created while purchase_orders already
            // contains identifiers from an earlier deployment. Skip any already persisted
            // PO id instead of returning a duplicate-key 409 to the caller.
            boolean alreadyExists = mongoTemplate.exists(
                    Query.query(Criteria.where("_id").is(candidate)),
                    com.recnaile.purchaseorder.domain.model.PurchaseOrder.class);
            if (!alreadyExists) {
                return candidate;
            }

            PurchaseOrderSequence nextSequence = mongoTemplate.findAndModify(
                    query, update, options, PurchaseOrderSequence.class);
            if (nextSequence == null) {
                throw new IllegalStateException("Unable to allocate purchase order number");
            }
            sequenceNumber = nextSequence.getSequence();
        }
    }

    public static String financialYear(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        int endYear = startYear + 1;
        return String.format("%02d-%02d", startYear % 100, endYear % 100);
    }
}
