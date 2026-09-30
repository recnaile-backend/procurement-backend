package com.recnaile.returns;

import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnDomainModelTest {

    @Test
    void newReturnRequestStartsPendingVerification() {
        Instant createdAt = Instant.parse("2026-09-30T00:00:00Z");

        ReturnRequest request = new ReturnRequest(
                "OUT/26-27/001",
                "OUT/26-27/001-L001",
                "ITEM-001",
                new BigDecimal("2"),
                "USER-001",
                ReturnCondition.GOOD,
                "Returned in working condition",
                "ASSET-001",
                createdAt
        );

        assertThat(request.getReturnId()).isNotBlank();
        assertThat(request.getStatus()).isEqualTo(ReturnStatus.PENDING_VERIFICATION);
        assertThat(request.getCondition()).isEqualTo(ReturnCondition.GOOD);
        assertThat(request.getQuantity()).isEqualByComparingTo("2");
        assertThat(request.getCreatedAt()).isEqualTo(createdAt);
        assertThat(request.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void conditionEnumContainsOnlyDocumentedReturnConditions() {
        assertThat(ReturnCondition.values())
                .containsExactly(ReturnCondition.GOOD, ReturnCondition.DAMAGED);
    }

    @Test
    void statusEnumContainsOnlyDefinedLifecycleStates() {
        assertThat(ReturnStatus.values())
                .containsExactly(ReturnStatus.PENDING_VERIFICATION, ReturnStatus.VERIFIED);
    }
}
