package com.recnaile.asset.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public static String fingerprint(Object... values) {
        String joined = java.util.Arrays.stream(values)
                .map(value -> value == null ? "<null>" : String.valueOf(value))
                .reduce((a, b) -> a + "\u001F" + b)
                .orElse("");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public <T> T execute(
            String rawKey,
            String operation,
            String requestHash,
            Supplier<T> action,
            Function<IdempotencyRecord, T> replay) {
        String key = normalize(rawKey);
        if (key == null) {
            return action.get();
        }

        IdempotencyRecord existing = repository.findByOperationAndIdempotencyKey(operation, key).orElse(null);
        if (existing != null) {
            return replayExisting(existing, requestHash, replay);
        }

        String id = fingerprint(operation, key);
        IdempotencyRecord claim = IdempotencyRecord.processing(id, operation, key, requestHash, Instant.now(clock));
        try {
            repository.insert(claim);
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord raced = repository.findByOperationAndIdempotencyKey(operation, key)
                    .orElseThrow(() -> new IllegalStateException("Idempotent request is already being processed; retry shortly"));
            return replayExisting(raced, requestHash, replay);
        }

        try {
            T result = action.get();
            String resourceType = result == null ? "NONE" : result.getClass().getSimpleName();
            String resourceId = resourceId(result);
            claim.complete(resourceType, resourceId, Instant.now(clock));
            repository.save(claim);
            return result;
        } catch (RuntimeException ex) {
            repository.deleteById(claim.getId());
            throw ex;
        }
    }

    private <T> T replayExisting(IdempotencyRecord record, String requestHash, Function<IdempotencyRecord, T> replay) {
        if (!Objects.equals(record.getRequestHash(), requestHash)) {
            throw new IllegalStateException("Idempotency-Key was already used with a different request");
        }
        if (!"COMPLETED".equals(record.getStatus())) {
            throw new IllegalStateException("Idempotent request is already being processed; retry shortly");
        }
        return replay.apply(record);
    }

    private static String normalize(String key) {
        if (key == null) return null;
        String value = key.trim();
        return value.isEmpty() ? null : value;
    }

    private static String resourceId(Object result) {
        if (result == null) return null;
        try {
            var method = result.getClass().getMethod("getAssignmentId");
            return String.valueOf(method.invoke(result));
        } catch (ReflectiveOperationException ignored) { }
        try {
            var method = result.getClass().getMethod("getAssetId");
            return String.valueOf(method.invoke(result));
        } catch (ReflectiveOperationException ignored) { }
        return null;
    }
}
