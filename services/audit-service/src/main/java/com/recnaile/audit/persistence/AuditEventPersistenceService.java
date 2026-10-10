package com.recnaile.audit.persistence;

import com.recnaile.audit.model.AuditEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/** Idempotent append-only persistence for audit events. */
@Service
@Validated
public class AuditEventPersistenceService {
    private final AuditEventMongoRepository repository;

    public AuditEventPersistenceService(AuditEventMongoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Appends a new event, or returns the stored event for an exact replay.
     * Reuse of an event ID with different content is rejected rather than silently ignored.
     */
    public AuditEventDocument append(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event is required");
        }
        AuditEventDocument candidate = AuditEventDocument.from(event);
        try {
            return repository.insert(candidate);
        } catch (DuplicateKeyException duplicateKey) {
            // The unique Mongo _id is the durable idempotency key. Check the stored payload
            // so an event-ID collision with different content is never treated as a replay.
            AuditEventDocument existing = repository.findById(candidate.eventId())
                    .orElseThrow(() -> duplicateKey);
            if (existing.equals(candidate)) {
                return existing;
            }
            throw new AuditEventIdentityConflictException(
                    "Audit event ID already exists with different content: " + candidate.eventId(), duplicateKey);
        }
    }
}
