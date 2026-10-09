package com.recnaile.returns.reservation;

import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReturnAllocationServiceTest {
    @Test void firstAcceptedReturnCreatesAllocation() {
        ReturnAllocationRepository repository = mock(ReturnAllocationRepository.class);
        when(repository.findByOutwardIdAndItemId("OUT%2F26-27%2F001", "ITEM-001")).thenReturn(Optional.empty());
        new ReturnAllocationService(repository).reserve(request(3), 5);
        ArgumentCaptor<ReturnAllocation> captor = ArgumentCaptor.forClass(ReturnAllocation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIssuedQuantity()).isEqualTo(5);
        assertThat(captor.getValue().getReservedQuantity()).isEqualTo(3);
    }

    @Test void rejectsCumulativeReturnBeyondIssuedQuantity() {
        ReturnAllocationRepository repository = mock(ReturnAllocationRepository.class);
        when(repository.findByOutwardIdAndItemId("OUT%2F26-27%2F001", "ITEM-001"))
                .thenReturn(Optional.of(new ReturnAllocation("OUT%2F26-27%2F001", "ITEM-001", 5, 4)));
        assertThatThrownBy(() -> new ReturnAllocationService(repository).reserve(request(2), 5))
                .isInstanceOf(ReturnRequestException.class)
                .hasMessage("Return quantity exceeds the remaining issued quantity");
        verify(repository, never()).save(any(ReturnAllocation.class));
    }

    private ReturnRequest request(int quantity) {
        return new ReturnRequest("OUT%2F26-27%2F001", "ITEM-001", quantity, "EMP-1", ReturnCondition.GOOD, null, Instant.parse("2026-10-08T00:00:00Z"));
    }
}
