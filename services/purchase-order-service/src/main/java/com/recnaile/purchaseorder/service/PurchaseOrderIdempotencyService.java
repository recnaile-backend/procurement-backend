package com.recnaile.purchaseorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.purchaseorder.domain.enums.IdempotencyStatus;
import com.recnaile.purchaseorder.domain.model.IdempotencyRecord;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.repository.IdempotencyRecordRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PurchaseOrderIdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public PurchaseOrderIdempotencyService(IdempotencyRecordRepository repository,
                                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String requestHash(String operation, String resourceId, Object request) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new Fingerprint(
                    operation,
                    resourceId,
                    request));
        } catch (JsonProcessingException ex) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_FINGERPRINT_FAILED",
                    "Unable to calculate the idempotency request fingerprint");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public Reservation reserve(String actorId,
                               String key,
                               String operation,
                               String requestHash) {
        String normalizedActor = normalizeActor(actorId);
        String normalizedKey = normalizeKey(key);

        Optional<IdempotencyRecord> existing =
                repository.findByActorIdAndKey(normalizedActor, normalizedKey);

        if (existing.isPresent()) {
            return new Reservation(existing.get(), false);
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setKey(normalizedKey);
        record.setActorId(normalizedActor);
        record.setOperation(operation);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyStatus.IN_PROGRESS);
        record.setCreatedAt(Instant.now());

        try {
            return new Reservation(repository.save(record), true);
        } catch (DuplicateKeyException ex) {
            return repository.findByActorIdAndKey(normalizedActor, normalizedKey)
                    .map(found -> new Reservation(found, false))
                    .orElseThrow(() -> new BusinessRuleException(
                            "IDEMPOTENCY_RESERVATION_FAILED",
                            "Unable to reserve the idempotency key"));
        }
    }

    public void assertSameRequest(IdempotencyRecord record,
                                  String operation,
                                  String requestHash) {
        if (!operation.equals(record.getOperation())
                || !requestHash.equals(record.getRequestHash())) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key has already been used with a different request");
        }

        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_REQUEST_IN_PROGRESS",
                    "The same idempotency request is already being processed");
        }
    }

    public void complete(IdempotencyRecord record,
                          String resourceId,
                          int responseStatus) {
        record.setResourceId(resourceId);
        record.setResponseStatus(responseStatus);
        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setCompletedAt(Instant.now());
        repository.save(record);
    }

    public void release(IdempotencyRecord record) {
        if (record != null && record.getId() != null) {
            repository.deleteById(record.getId());
        }
    }

    public Optional<IdempotencyRecord> find(String actorId, String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return repository.findByActorIdAndKey(normalizeActor(actorId), normalizeKey(key));
    }

    private String normalizeActor(String actorId) {
        return actorId == null || actorId.isBlank() ? "unknown-actor" : actorId.trim();
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key header is required for this operation");
        }
        String normalized = key.trim();
        if (normalized.length() > 200) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key must not exceed 200 characters");
        }
        return normalized;
    }

    public record Reservation(IdempotencyRecord record, boolean newlyReserved) {}

    private record Fingerprint(String operation, String resourceId, Object request) {}
}
