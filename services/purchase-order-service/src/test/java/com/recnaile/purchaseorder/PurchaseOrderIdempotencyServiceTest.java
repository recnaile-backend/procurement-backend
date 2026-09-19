package com.recnaile.purchaseorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.purchaseorder.domain.enums.IdempotencyStatus;
import com.recnaile.purchaseorder.domain.model.IdempotencyRecord;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.repository.IdempotencyRecordRepository;
import com.recnaile.purchaseorder.service.PurchaseOrderIdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderIdempotencyServiceTest {

    @Mock
    IdempotencyRecordRepository repository;

    @Test
    void shouldRejectSameKeyWhenRequestFingerprintChanges() {
        PurchaseOrderIdempotencyService service =
                new PurchaseOrderIdempotencyService(repository, new ObjectMapper());

        IdempotencyRecord record = new IdempotencyRecord();
        record.setOperation("TRANSITION_ISSUED");
        record.setRequestHash("original");
        record.setStatus(IdempotencyStatus.COMPLETED);

        assertThatThrownBy(() ->
                service.assertSameRequest(record, "TRANSITION_ISSUED", "different"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
    }

    @Test
    void shouldRejectConcurrentReuseWhileRequestIsInProgress() {
        PurchaseOrderIdempotencyService service =
                new PurchaseOrderIdempotencyService(repository, new ObjectMapper());

        IdempotencyRecord record = new IdempotencyRecord();
        record.setOperation("TRANSITION_ISSUED");
        record.setRequestHash("same");
        record.setStatus(IdempotencyStatus.IN_PROGRESS);

        assertThatThrownBy(() ->
                service.assertSameRequest(record, "TRANSITION_ISSUED", "same"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already being processed");
    }

    @Test
    void shouldReturnExistingRecordWhenKeyWasReservedByAnotherRequest() {
        PurchaseOrderIdempotencyService service =
                new PurchaseOrderIdempotencyService(repository, new ObjectMapper());

        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setKey("key-1");
        existing.setActorId("user-1");
        existing.setOperation("CREATE_FROM_APPROVED_INDENT");
        existing.setRequestHash("hash");
        existing.setStatus(IdempotencyStatus.COMPLETED);

        when(repository.findByActorIdAndKey("user-1", "key-1"))
                .thenReturn(Optional.of(existing));

        var reservation = service.reserve("user-1", "key-1",
                "CREATE_FROM_APPROVED_INDENT", "hash");

        assertThat(reservation.newlyReserved()).isFalse();
        assertThat(reservation.record()).isSameAs(existing);
    }
}
