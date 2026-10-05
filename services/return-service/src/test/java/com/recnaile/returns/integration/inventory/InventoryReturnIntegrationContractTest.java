package com.recnaile.returns.integration.inventory;

import com.recnaile.returns.event.ReturnRecordedEvent;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReturnIntegrationContractTest {

    @Test
    void goodReturnEventRepresentsAvailableStockIncrease() {
        ReturnRecordedEvent event = event(ReturnCondition.GOOD);

        assertThat(event.getEventType()).isEqualTo("RETURN_RECORDED");
        assertThat(event.getItemId()).isEqualTo("ITEM-001");
        assertThat(event.getQty()).isEqualTo(3);
        assertThat(event.getCondition()).isEqualTo(ReturnCondition.GOOD);
    }

    @Test
    void damagedReturnEventRepresentsDefectStockIncrease() {
        ReturnRecordedEvent event = event(ReturnCondition.DAMAGED);

        assertThat(event.getEventType()).isEqualTo("RETURN_RECORDED");
        assertThat(event.getItemId()).isEqualTo("ITEM-001");
        assertThat(event.getQty()).isEqualTo(2);
        assertThat(event.getCondition()).isEqualTo(ReturnCondition.DAMAGED);
    }

    @Test
    void eventContainsStableReturnIdentityAndOccurrenceTime() {
        Instant occurredAt = Instant.parse("2026-09-30T05:00:00Z");
        ReturnRequest request = new ReturnRequest(
                "OUT-001", "ITEM-001",
                1, "USER-001", ReturnCondition.GOOD,
                "returned", occurredAt);
        request.setReturnId("RET-001");
        request.setStatus(ReturnStatus.ACCEPTED_GOOD);
        request.setUpdatedAt(occurredAt);
        request.setVersion(0L);

        ReturnRecordedEvent event = ReturnRecordedEvent.create(request);

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getReturnId()).isEqualTo("RET-001");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }

    private ReturnRecordedEvent event(ReturnCondition condition) {
        Instant occurredAt = Instant.parse("2026-09-30T05:00:00Z");
        ReturnRequest request = new ReturnRequest(
                "OUT-001", "ITEM-001",
                condition == ReturnCondition.GOOD ? 3 : 2,
                "USER-001", condition, "returned", occurredAt);
        request.setReturnId("RET-001");
        request.setStatus(condition == ReturnCondition.GOOD
                ? ReturnStatus.ACCEPTED_GOOD
                : ReturnStatus.ACCEPTED_DAMAGED);
        request.setUpdatedAt(occurredAt);
        request.setVersion(0L);
        return ReturnRecordedEvent.create(request);
    }
}
