package com.recnaile.indent.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.domain.model.IdempotencyRecord;
import com.recnaile.indent.exception.IdempotencyKeyConflictException;
import com.recnaile.indent.repository.IdempotencyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Finds an existing idempotency record for the authenticated actor,
     * operation and supplied idempotency key.
     */
    public Optional<IdempotencyRecord> find(
            String actorId,
            String operation,
            String key) {

        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        return idempotencyRepository
                .findByActorIdAndOperationAndKey(
                        actorId,
                        operation,
                        key);
    }

    /**
     * Creates a deterministic SHA-256 hash of the request payload.
     *
     * The hash allows us to detect a client reusing the same
     * Idempotency-Key for a different request.
     */
    public String requestHash(Object request) {

        try {
            byte[] payload =
                    objectMapper.writeValueAsBytes(request);

            byte[] digest =
                    MessageDigest
                            .getInstance("SHA-256")
                            .digest(payload);

            return HexFormat.of().formatHex(digest);

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException(
                    "Unable to serialize request for idempotency validation",
                    ex);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    ex);
        }
    }

    /**
     * Ensures that an existing idempotency key is being reused
     * with exactly the same request payload.
     */
    public void assertSameRequest(
            IdempotencyRecord existingRecord,
            String requestHash) {

        if (!requestHash.equals(existingRecord.getRequestHash())) {

            throw new IdempotencyKeyConflictException(
                    "Idempotency-Key has already been used with a different request");
        }
    }

    /**
     * Stores the successful result associated with an idempotency key.
     */
    public IdempotencyRecord save(
            String actorId,
            String operation,
            String key,
            String requestHash,
            String resourceId,
            Object response,
            int responseStatus,
            Duration ttl) {

        try {

            Instant now = Instant.now();

            IdempotencyRecord record =
                    IdempotencyRecord.builder()
                            .key(key)
                            .actorId(actorId)
                            .operation(operation)
                            .resourceId(resourceId)
                            .requestHash(requestHash)
                            .responseStatus(responseStatus)
                            .responseBody(
                                    objectMapper.writeValueAsString(response))
                            .createdAt(now)
                            .expiresAt(now.plus(ttl))
                            .build();

            return idempotencyRepository.save(record);

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException(
                    "Unable to serialize idempotency response",
                    ex);
        }
    }
}