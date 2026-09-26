package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.outward.domain.enums.IdempotencyStatus;
import com.recnaile.outward.domain.model.IdempotencyRecord;
import com.recnaile.outward.exception.IdempotencyConflictException;
import com.recnaile.outward.repository.IdempotencyRepository;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {

    @Test
    void sameCompletedKeyReplays() {
        IdempotencyRepository repository = mock(IdempotencyRepository.class);
        var request = java.util.Map.of("itemId", "ITEM-1");
        String fingerprint;
        try {
            fingerprint = java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(new ObjectMapper().writeValueAsBytes(request)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
        IdempotencyRecord record = new IdempotencyRecord(
                "u|POST:/api/v1/outward|key", "key",
                "POST:/api/v1/outward", "u",
                fingerprint);
        record.complete("OUT-1", java.time.Instant.now());
        when(repository.findByScopeKey(any())).thenReturn(Optional.of(record));

        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());

        assertEquals("OUT-1",
                service.existingOrReserve("u", "POST:/api/v1/outward", "key", request).orElseThrow());
    }

    @Test
    void differentPayloadWithSameKeyIsRejected() {
        IdempotencyRepository repository = mock(IdempotencyRepository.class);
        IdempotencyRecord record = new IdempotencyRecord(
                "u|POST:/api/v1/outward|key", "key",
                "POST:/api/v1/outward", "u",
                "not-the-same-hash");
        when(repository.findByScopeKey(any())).thenReturn(Optional.of(record));

        IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());

        assertThrows(IdempotencyConflictException.class,
                () -> service.existingOrReserve("u", "POST:/api/v1/outward", "key",
                        java.util.Map.of("itemId", "ITEM-1")));
    }
}
