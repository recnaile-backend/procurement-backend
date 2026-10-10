package com.recnaile.audit.application;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.persistence.AuditEventDocument;
import com.recnaile.audit.persistence.AuditEventPersistenceService;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Application use case for durable, idempotent audit-event ingestion. */
@Service
public class AuditEventIngestionService {
    private final AuditEventPersistenceService persistenceService;

    public AuditEventIngestionService(AuditEventPersistenceService persistenceService) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "persistenceService");
    }

    /** Persists an event without exposing repository details to messaging adapters. */
    public AuditEventDocument ingest(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event is required");
        }
        return persistenceService.append(event);
    }
}
