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
    void goodReturnEventContainsOutwardIdentityAndGoodCondition() {
        ReturnRecordedEvent event = event(ReturnCondition.GOOD, 3);

        assertThat(event.eventType()).isEqualTo("RETURN_RECORDED");
        assertThat(event.returnId()).isEqualTo("RETURN%2F26-27%2F001");
        assertThat(event.outwardId()).isEqualTo("OUT%2F26-27%2F001");
        assertThat(event.itemId()).isEqualTo("ITEM-001");
        assertThat(event.qty()).isEqualTo(3);
        assertThat(event.condition()).isEqualTo(ReturnCondition.GOOD);
    }

    @Test
    void damagedReturnEventContainsOutwardIdentityAndDamagedCondition() {
        ReturnRecordedEvent event = event(ReturnCondition.DAMAGED, 2);

        assertThat(event.eventType()).isEqualTo("RETURN_RECORDED");
        assertThat(event.outwardId()).isEqualTo("OUT%2F26-27%2F001");
        assertThat(event.itemId()).isEqualTo("ITEM-001");
        assertThat(event.qty()).isEqualTo(2);
        assertThat(event.condition()).isEqualTo(ReturnCondition.DAMAGED);
    }

    @Test
    void eventContainsStableReturnIdentityAndOccurrenceTime() {
        Instant occurredAt = Instant.parse("2026-09-30T05:00:00Z");
        ReturnRequest request = new ReturnRequest(
                "OUT%2F26-27%2F001", "ITEM-001",
                1, "USER-001", ReturnCondition.GOOD,
                "returned", occurredAt);
        request.setReturnId("RETURN%2F26-27%2F001");
        request.setStatus(ReturnStatus.ACCEPTED_GOOD);
        request.setUpdatedAt(occurredAt);

        ReturnRecordedEvent event = ReturnRecordedEvent.create(request);

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.returnId()).isEqualTo("RETURN%2F26-27%2F001");
        assertThat(event.outwardId()).isEqualTo("OUT%2F26-27%2F001");
        assertThat(event.occurredAt()).isEqualTo(occurredAt);
    }

    private ReturnRecordedEvent event(ReturnCondition condition, int quantity) {
        Instant occurredAt = Instant.parse("2026-09-30T05:00:00Z");
        ReturnRequest request = new ReturnRequest(
                "OUT%2F26-27%2F001", "ITEM-001",
                quantity, "USER-001", condition,
                "returned", occurredAt);
        request.setReturnId("RETURN%2F26-27%2F001");
        request.setStatus(condition == ReturnCondition.GOOD
                ? ReturnStatus.ACCEPTED_GOOD
                : ReturnStatus.ACCEPTED_DAMAGED);
        request.setUpdatedAt(occurredAt);
        return ReturnRecordedEvent.create(request);
    }
}
