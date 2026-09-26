package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.model.PurchaseOrderSequence;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.service.PurchaseOrderNumberGenerator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderNumberGeneratorTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Test
    void generatesFirstBackendPoId() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO-26-27", 1));
        when(mongoTemplate.exists(any(Query.class), eq(PurchaseOrder.class))).thenReturn(false);

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(
                mongoTemplate, Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneId.of("Asia/Kolkata")));

        assertThat(generator.nextNumber()).isEqualTo("PO%2F26-27%2F001");
    }

    @Test
    void incrementsBackendSequence() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO-26-27", 42));
        when(mongoTemplate.exists(any(Query.class), eq(PurchaseOrder.class))).thenReturn(false);

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(
                mongoTemplate, Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneId.of("Asia/Kolkata")));

        assertThat(generator.nextNumber()).isEqualTo("PO%2F26-27%2F042");
    }

    @Test
    void skipsExistingPoIdWhenSequenceDocumentStartsBehindExistingData() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO-26-27", 1))
                .thenReturn(new PurchaseOrderSequence("PO-26-27", 2));
        when(mongoTemplate.exists(any(Query.class), eq(PurchaseOrder.class)))
                .thenReturn(true)
                .thenReturn(false);

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(
                mongoTemplate, Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneId.of("Asia/Kolkata")));

        assertThat(generator.nextNumber()).isEqualTo("PO%2F26-27%2F002");
    }

    @Test
    void financialYearChangesOnAprilFirst() {
        assertThat(PurchaseOrderNumberGenerator.financialYear(java.time.LocalDate.of(2026, 3, 31)))
                .isEqualTo("25-26");
        assertThat(PurchaseOrderNumberGenerator.financialYear(java.time.LocalDate.of(2026, 4, 1)))
                .isEqualTo("26-27");
    }
}
