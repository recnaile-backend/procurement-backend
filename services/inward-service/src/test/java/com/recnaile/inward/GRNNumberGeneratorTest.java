package com.recnaile.inward;

import com.recnaile.inward.model.GRNSequence;
import com.recnaile.inward.service.GRNNumberGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GRNNumberGeneratorTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final GRNNumberGenerator generator = new GRNNumberGenerator(mongoTemplate);

    @Test
    void generatesSequentialBusinessNumbersForCurrentFinancialYear() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(GRNSequence.class)))
                .thenReturn(new GRNSequence("26-27", 1L))
                .thenReturn(new GRNSequence("26-27", 2L))
                .thenReturn(new GRNSequence("26-27", 3L));

        assertEquals("GRN/26-27/001", generator.next(LocalDate.of(2026, 9, 25)));
        assertEquals("GRN/26-27/002", generator.next(LocalDate.of(2026, 9, 25)));
        assertEquals("GRN/26-27/003", generator.next(LocalDate.of(2026, 9, 25)));
    }

    @Test
    void usesPreviousFinancialYearBeforeApril() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(GRNSequence.class)))
                .thenReturn(new GRNSequence("25-26", 7L));

        assertEquals("GRN/25-26/007", generator.next(LocalDate.of(2026, 3, 31)));
    }

    @Test
    void startsNewSequenceForNewFinancialYear() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(GRNSequence.class)))
                .thenReturn(new GRNSequence("26-27", 1L));

        assertEquals("GRN/26-27/001", generator.next(LocalDate.of(2026, 4, 1)));
    }
}
