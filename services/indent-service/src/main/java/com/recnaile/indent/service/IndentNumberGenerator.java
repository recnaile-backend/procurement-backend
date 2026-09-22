package com.recnaile.indent.service;

import com.recnaile.indent.domain.model.IndentSequence;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;

@Service
public class IndentNumberGenerator {

    private final MongoTemplate mongoTemplate;

    public IndentNumberGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String nextIndentNumber(LocalDate date) {
        String financialYear = financialYear(date);

        Query query = Query.query(
                Criteria.where("_id").is(financialYear)
        );

        Update update = new Update().inc("sequence", 1);

        IndentSequence sequence =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        org.springframework.data.mongodb.core.FindAndModifyOptions.options()
                                .returnNew(true)
                                .upsert(true),
                        IndentSequence.class
                );

        if (sequence == null) {
            throw new IllegalStateException("Unable to generate indent number");
        }

        return String.format(
                "IN%%2F%s%%2F%03d",
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
