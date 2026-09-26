package com.recnaile.inward.service;

import com.recnaile.inward.model.GRNSequence;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;

/**
 * Allocates the backend Goods Receipt Note identifier atomically.
 *
 * Canonical business format:
 *     GRN/26-27/001
 *
 * The sequence is maintained independently for each Indian financial year
 * (April through March), matching the Purchase Order numbering convention.
 * When the identifier is used in a URL path, the slash separators are URL-encoded
 * as %2F (for example GRN%2F26-27%2F001). The stored/business identifier remains
 * the literal GRN/26-27/001 value.
 */
@Service
public class GRNNumberGenerator {

    private static final String NUMBER_PREFIX = "GRN";
    private static final int NUMBER_WIDTH = 3;

    private final MongoTemplate mongoTemplate;

    public GRNNumberGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String next() {
        return next(LocalDate.now());
    }

    public String next(LocalDate date) {
        String financialYear = financialYear(date);

        Query query = Query.query(
                Criteria.where("_id").is(financialYear)
        );

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

        return String.format(
                "%s/%s/%0" + NUMBER_WIDTH + "d",
                NUMBER_PREFIX,
                financialYear,
                sequence.getSequence()
        );
    }

    private String financialYear(LocalDate date) {
        int startYear = date.getMonthValue() >= Month.APRIL.getValue()
                ? date.getYear()
                : date.getYear() - 1;

        return String.format(
                "%02d-%02d",
                startYear % 100,
                (startYear + 1) % 100
        );
    }
}
