package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.service.PurchaseOrderNumberGenerator;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderNumberGeneratorTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Test
    void generatesFirstNumberForYear() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO:2026", 1));

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(mongoTemplate);

        assertThat(generator.nextNumber(LocalDate.of(2026, 9, 19)))
                .isEqualTo("PO-2026-0001");

        verify(mongoTemplate).findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class));
    }

    @Test
    void incrementsSequenceWithinSameYear() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO:2026", 42));

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(mongoTemplate);

        assertThat(generator.nextNumber(LocalDate.of(2026, 12, 31)))
                .isEqualTo("PO-2026-0042");
    }

    @Test
    void startsNewYearSequenceAtOne() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO:2027", 1));

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(mongoTemplate);

        assertThat(generator.nextNumber(LocalDate.of(2027, 1, 1)))
                .isEqualTo("PO-2027-0001");
    }

    @Test
    void defaultDateUsesConfiguredClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC);
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO:2026", 7));

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(mongoTemplate, clock);

        assertThat(generator.nextNumber()).isEqualTo("PO-2026-0007");
    }
}
