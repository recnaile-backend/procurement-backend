package com.recnaile.outward.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.outward.domain.enums.IdempotencyStatus;
import com.recnaile.outward.domain.model.IdempotencyRecord;
import com.recnaile.outward.exception.IdempotencyConflictException;
import com.recnaile.outward.exception.IdempotencyInProgressException;
import com.recnaile.outward.repository.IdempotencyRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<String> existingOrReserve(String principal, String endpoint,
                                              String idempotencyKey, Object request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        String fingerprint = fingerprint(request);
        String scopeKey = principal + "|" + endpoint + "|" + idempotencyKey;

        Optional<IdempotencyRecord> existing = repository.findByScopeKey(scopeKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!fingerprint.equals(record.getRequestFingerprint())) {
                throw new IdempotencyConflictException();
            }
            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                return Optional.ofNullable(record.getOutwardId());
            }
            throw new IdempotencyInProgressException();
        }

        try {
            repository.save(new IdempotencyRecord(scopeKey, idempotencyKey, endpoint,
                    principal, fingerprint));
            return Optional.empty();
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord race = repository.findByScopeKey(scopeKey)
                    .orElseThrow(() -> new IdempotencyInProgressException());
            if (!fingerprint.equals(race.getRequestFingerprint())) {
                throw new IdempotencyConflictException();
            }
            if (race.getStatus() == IdempotencyStatus.COMPLETED) {
                return Optional.ofNullable(race.getOutwardId());
            }
            throw new IdempotencyInProgressException();
        }
    }

    public void complete(String principal, String endpoint, String idempotencyKey,
                         Object request, String outwardId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        String scopeKey = principal + "|" + endpoint + "|" + idempotencyKey;
        IdempotencyRecord record = repository.findByScopeKey(scopeKey)
                .orElseThrow(() -> new IllegalStateException("Idempotency reservation not found"));
        record.complete(outwardId, Instant.now());
        repository.save(record);
    }

    private String fingerprint(Object request) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to fingerprint idempotent request", ex);
        }
    }
}
