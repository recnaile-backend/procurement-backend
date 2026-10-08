package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnAcceptedEventTest {

    @Test
    void eventCarriesReturnAndOutwardIdentityForAssetService() {
        Instant occurredAt = Instant.parse("2026-09-30T06:00:00Z");
        ReturnRequest request = new ReturnRequest(
                "OUT%2F26-27%2F001", "ITEM-1", 1,
                "EMP-1", ReturnCondition.DAMAGED, "damaged",
                occurredAt, true);
        request.setReturnId("RETURN%2F26-27%2F001");
        request.setUpdatedAt(occurredAt);

        ReturnAcceptedEvent event = ReturnAcceptedEvent.create(request);

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventType()).isEqualTo("ReturnAccepted");
        assertThat(event.returnId()).isEqualTo("RETURN%2F26-27%2F001");
        assertThat(event.outwardId()).isEqualTo("OUT%2F26-27%2F001");
        assertThat(event.itemId()).isEqualTo("ITEM-1");
        assertThat(event.qty()).isEqualTo(1);
        assertThat(event.condition()).isEqualTo(ReturnCondition.DAMAGED);
    }
}
