package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.model.PurchaseOrderSequence;
import com.recnaile.purchaseorder.service.PurchaseOrderNumberGenerator;
import org.junit.jupiter.api.Test;
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

@ExtendWith(MockitoExtension.class)
class PurchaseOrderNumberGeneratorTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Test
    void generatesFirstBackendPoId() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO", 1));

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(mongoTemplate);

        assertThat(generator.nextNumber()).isEqualTo("PO-001");
    }

    @Test
    void incrementsBackendSequence() {
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PurchaseOrderSequence.class)))
                .thenReturn(new PurchaseOrderSequence("PO", 42));

        PurchaseOrderNumberGenerator generator = new PurchaseOrderNumberGenerator(mongoTemplate);

        assertThat(generator.nextNumber()).isEqualTo("PO-042");
    }
}
