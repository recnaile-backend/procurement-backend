package com.recnaile.audit.persistence;

import com.recnaile.audit.model.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/** Persists validated audit events without updating an existing event with the same identity. */
@Service
@Validated
public class AuditEventPersistenceService {
    private final AuditEventMongoRepository repository;

    public AuditEventPersistenceService(AuditEventMongoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public AuditEventDocument append(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event is required");
        }
        // insert() intentionally fails on a duplicate _id instead of overwriting audit history.
        return repository.insert(AuditEventDocument.from(event));
    }
}
