package com.recnaile.inward.service;

import com.recnaile.inward.model.GRNSequence;
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
 * Allocates the backend Goods Receipt Note identifier atomically.
 *
 * The business identifier is stored exactly as generated, using the same
 * convention as the Purchase Order Service:
 * {@code GRN%2F26-27%2F001}.
 *
 * The sequence is maintained independently for each Indian financial year
 * (April through March). The sequence restarts at 001 for each financial year.
 */
@Service
public class GRNNumberGenerator {

    private static final String NUMBER_PREFIX = "GRN%2F";
    private static final String NUMBER_SEPARATOR = "%2F";
    private static final int NUMBER_WIDTH = 3;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public GRNNumberGenerator(MongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public String next() {
        return next(LocalDate.now(clock.withZone(BUSINESS_ZONE)));
    }

    public String next(LocalDate date) {
        String financialYear = financialYear(date);
        String sequenceId = "GRN-" + financialYear;

        Query query = Query.query(Criteria.where("_id").is(sequenceId));
        Update update = new Update().inc("sequence", 1L);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);

        GRNSequence sequence = mongoTemplate.findAndModify(
                query,
                update,
                options,
                GRNSequence.class
        );

        if (sequence == null) {
            throw new IllegalStateException("Unable to allocate GRN number");
        }

        long sequenceNumber = sequence.getSequence();
        while (true) {
            String candidate = NUMBER_PREFIX + financialYear + NUMBER_SEPARATOR
                    + String.format("%0" + NUMBER_WIDTH + "d", sequenceNumber);

            // If the sequence document is behind already persisted GRNs, skip the
            // existing identifier rather than returning a duplicate-key error.
            boolean alreadyExists = mongoTemplate.exists(
                    Query.query(Criteria.where("grnId").is(candidate)),
                    com.recnaile.inward.model.GRNDocument.class);

            if (!alreadyExists) {
                return candidate;
            }

            GRNSequence nextSequence = mongoTemplate.findAndModify(
                    query,
                    update,
                    options,
                    GRNSequence.class
            );

            if (nextSequence == null) {
                throw new IllegalStateException("Unable to allocate GRN number");
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
