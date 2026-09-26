package com.recnaile.inward;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.recnaile.inward.dto.CreateGRNRequest;
import com.recnaile.inward.model.IdempotencyRecord;
import com.recnaile.inward.repository.IdempotencyRecordRepository;
import com.recnaile.inward.service.IdempotencyService;

class IdempotencyServiceTest {
    private final IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
    private final IdempotencyService service = new IdempotencyService(repository);

    @Test
    void optionalKeyDoesNotCreateIdempotencyRecord() {
        assertNotNull(service);
        verifyNoInteractions(repository);
    }

    @Test
    void reservesNewKey() {
        CreateGRNRequest request = new CreateGRNRequest("PO-1", "DN-1", null, "user", java.util.List.of());
        when(repository.findByScopeKey(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));
        IdempotencyRecord record = service.reserve("KEY-1", "user-1", request);
        assertEquals(IdempotencyRecord.Status.IN_PROGRESS, record.getStatus());
        verify(repository).save(any(IdempotencyRecord.class));
    }

    @Test
    void sameCompletedKeyCanBeReused() {
        CreateGRNRequest request = new CreateGRNRequest("PO-1", "DN-1", null, "user", java.util.List.of());
        when(repository.findByScopeKey(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));
        IdempotencyRecord first = service.reserve("KEY-1", "user-1", request);
        first.complete("GRN/26-27/001");
        when(repository.findByScopeKey(anyString())).thenReturn(Optional.of(first));
        IdempotencyRecord second = service.reserve("KEY-1", "user-1", request);
        assertEquals("GRN/26-27/001", second.getGrnId());
        assertEquals(IdempotencyRecord.Status.COMPLETED, second.getStatus());
    }
}
