package com.recnaile.asset.idempotency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock IdempotencyRecordRepository repository;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void missingKeyExecutesNormally() {
        String result = service.execute(null, "CREATE_ASSET", "hash", () -> "created", record -> "replayed");
        assertEquals("created", result);
        verifyNoInteractions(repository);
    }

    @Test
    void completedKeyReplaysWithoutExecutingAction() {
        IdempotencyRecord record = IdempotencyRecord.processing("id", "CREATE_ASSET", "key", "hash", Instant.now());
        record.complete("Asset", "ASSET%2F26-27%2F001", Instant.now());
        when(repository.findByOperationAndIdempotencyKey("CREATE_ASSET", "key")).thenReturn(Optional.of(record));

        String result = service.execute("key", "CREATE_ASSET", "hash", () -> "wrong",
                existing -> "replayed:" + existing.getResourceId());

        assertEquals("replayed:ASSET%2F26-27%2F001", result);
    }

    @Test
    void reusingKeyWithDifferentPayloadIsRejected() {
        IdempotencyRecord record = IdempotencyRecord.processing("id", "CREATE_ASSET", "key", "original", Instant.now());
        record.complete("Asset", "ASSET%2F26-27%2F001", Instant.now());
        when(repository.findByOperationAndIdempotencyKey("CREATE_ASSET", "key")).thenReturn(Optional.of(record));

        assertThrows(IllegalStateException.class, () -> service.execute("key", "CREATE_ASSET", "different",
                () -> "created", existing -> "replayed"));
    }

    @Test
    void successfulRequestIsClaimedAndCompleted() {
        when(repository.findByOperationAndIdempotencyKey("CREATE_ASSET", "key")).thenReturn(Optional.empty());

        String result = service.execute("key", "CREATE_ASSET", "hash", () -> "created", existing -> "replayed");

        assertEquals("created", result);
        verify(repository).insert(any(IdempotencyRecord.class));
        verify(repository).save(any(IdempotencyRecord.class));
    }
}
