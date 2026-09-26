package com.recnaile.inward.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.recnaile.inward.dto.CreateGRNRequest;
import com.recnaile.inward.exception.IdempotencyConflictException;
import com.recnaile.inward.exception.IdempotencyInProgressException;
import com.recnaile.inward.model.IdempotencyRecord;
import com.recnaile.inward.repository.IdempotencyRecordRepository;

@Service
public class IdempotencyService {

    private static final String ENDPOINT = "POST:/api/v1/goods-receipts";
    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public IdempotencyRecord reserve(String key, String principal, CreateGRNRequest request) {
        String normalizedKey = key.trim();
        String normalizedPrincipal = principal == null || principal.isBlank() ? "UNKNOWN" : principal;
        String scopeKey = ENDPOINT + "|" + normalizedPrincipal + "|" + normalizedKey;
        String fingerprint = sha256(request.toString());

        return repository.findByScopeKey(scopeKey).map(existing -> {
            if (!existing.getRequestFingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(
                        "The Idempotency-Key has already been used with a different request.");
            }
            if (existing.getStatus() == IdempotencyRecord.Status.IN_PROGRESS) {
                throw new IdempotencyInProgressException(
                        "A request with this Idempotency-Key is already being processed.");
            }
            return existing;
        }).orElseGet(() -> {
            try {
                return repository.save(new IdempotencyRecord(
                        scopeKey, normalizedKey, ENDPOINT, normalizedPrincipal, fingerprint));
            } catch (DuplicateKeyException ex) {
                IdempotencyRecord existing = repository.findByScopeKey(scopeKey)
                        .orElseThrow(() -> ex);
                if (!existing.getRequestFingerprint().equals(fingerprint)) {
                    throw new IdempotencyConflictException(
                            "The Idempotency-Key has already been used with a different request.");
                }
                if (existing.getStatus() == IdempotencyRecord.Status.IN_PROGRESS) {
                    throw new IdempotencyInProgressException(
                            "A request with this Idempotency-Key is already being processed.");
                }
                return existing;
            }
        });
    }

    public void complete(IdempotencyRecord record, String grnId) {
        record.complete(grnId);
        repository.save(record);
    }

    public void release(IdempotencyRecord record) {
        repository.delete(record);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
