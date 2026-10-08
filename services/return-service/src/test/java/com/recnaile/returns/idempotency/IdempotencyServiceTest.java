package com.recnaile.returns.idempotency;

import com.recnaile.returns.exception.IdempotencyConflictException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {
    @Test
    void conflictingReuseIsRejected() {
        IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
        ReturnRequestRepository returnRepository = mock(ReturnRequestRepository.class);
        IdempotencyService service = new IdempotencyService(repository, returnRepository);
        IdempotencyRecord record = new IdempotencyRecord("key-1", "CREATE_RETURN", "user-1", "hash-a", "RETURN%2F26-27%2F001", Instant.now());
        when(repository.findById("key-1")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.findExisting("key-1", "CREATE_RETURN", "user-1", "hash-b"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void matchingReuseReturnsOriginalReturn() {
        IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
        ReturnRequestRepository returnRepository = mock(ReturnRequestRepository.class);
        IdempotencyService service = new IdempotencyService(repository, returnRepository);
        IdempotencyRecord record = new IdempotencyRecord("key-1", "CREATE_RETURN", "user-1", "hash-a", "RETURN%2F26-27%2F001", Instant.now());
        ReturnRequest request = new ReturnRequest("OUT-1", "ITEM-1", 1,
                "user-1", ReturnCondition.GOOD, null, Instant.now());
        request.setReturnId("RETURN%2F26-27%2F001");
        when(repository.findById("key-1")).thenReturn(Optional.of(record));
        when(returnRepository.findByReturnId("RETURN%2F26-27%2F001")).thenReturn(Optional.of(request));

        service.findExisting("key-1", "CREATE_RETURN", "user-1", "hash-a");
        verify(returnRepository).findByReturnId("RETURN%2F26-27%2F001");
    }
}
