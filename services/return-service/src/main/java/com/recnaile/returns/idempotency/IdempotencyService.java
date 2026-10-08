package com.recnaile.returns.idempotency;

import com.recnaile.returns.exception.IdempotencyConflictException;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ReturnRequestRepository returnRepository;
    private final Clock clock;

    public IdempotencyService(
            IdempotencyRecordRepository repository,
            ReturnRequestRepository returnRepository) {
        this.repository = repository;
        this.returnRepository = returnRepository;
        this.clock = Clock.systemUTC();
    }

    public Optional<ReturnRequest> findExisting(
            String key,
            String operation,
            String actor,
            String requestHash) {
        String normalized = normalizeKey(key);
        return repository.findById(normalized).map(record -> {
            validate(record, operation, actor, requestHash);
            if (record.getReturnId() == null) {
                throw new IdempotencyConflictException(
                        "The same Idempotency-Key is currently being processed");
            }
            return returnRepository.findByReturnId(record.getReturnId())
                    .orElseThrow(() -> new ReturnNotFoundException(record.getReturnId()));
        });
    }

    /**
     * Atomically claims an idempotency key before the business mutation.
     * The unique MongoDB _id prevents concurrent requests from both executing
     * the same state-changing operation.
     */
    public void claim(
            String key,
            String operation,
            String actor,
            String requestHash) {
        String normalized = normalizeKey(key);
        try {
            repository.save(new IdempotencyRecord(
                    normalized,
                    operation,
                    actor,
                    requestHash,
                    null,
                    Instant.now(clock)));
        } catch (DuplicateKeyException duplicate) {
            IdempotencyRecord existing = repository.findById(normalized)
                    .orElseThrow(() -> duplicate);
            validate(existing, operation, actor, requestHash);
            if (existing.getReturnId() == null) {
                throw new IdempotencyConflictException(
                        "The same Idempotency-Key is currently being processed");
            }
        }
    }

    public void complete(
            String key,
            String operation,
            String actor,
            String requestHash,
            String returnId) {
        String normalized = normalizeKey(key);
        IdempotencyRecord existing = repository.findById(normalized)
                .orElseThrow(() ->
                        new IllegalStateException("Idempotency claim was not found"));

        validate(existing, operation, actor, requestHash);
        if (existing.getReturnId() != null
                && !existing.getReturnId().equals(returnId)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key is already associated with another return");
        }

        existing.setReturnId(returnId);
        repository.save(existing);
    }

    /**
     * Backward-compatible completion operation for service/unit callers.
     */
    public void record(
            String key,
            String operation,
            String actor,
            String requestHash,
            String returnId) {
        try {
            complete(key, operation, actor, requestHash, returnId);
        } catch (IllegalStateException missingClaim) {
            claim(key, operation, actor, requestHash);
            complete(key, operation, actor, requestHash, returnId);
        }
    }

    public String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        String normalized = key.trim();
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 200 characters");
        }
        return normalized;
    }

    public String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validate(
            IdempotencyRecord record,
            String operation,
            String actor,
            String requestHash) {
        if (!operation.equals(record.getOperation())
                || !actor.equals(record.getActor())
                || !requestHash.equals(record.getRequestHash())) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used with different request data");
        }
    }
}
