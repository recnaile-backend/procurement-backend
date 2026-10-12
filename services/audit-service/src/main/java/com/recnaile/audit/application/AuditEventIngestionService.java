package com.recnaile.audit.application;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.persistence.AuditEventDocument;
import com.recnaile.audit.persistence.AuditEventPersistenceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Application use case for durable, idempotent audit-event ingestion. */
@Service
public class AuditEventIngestionService {
    private final AuditEventPersistenceService persistenceService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public AuditEventIngestionService(AuditEventPersistenceService persistenceService, MeterRegistry meterRegistry) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "persistenceService");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    /** Retained for isolated unit tests and simple callers outside the Spring context. */
    public AuditEventIngestionService(AuditEventPersistenceService persistenceService) {
        this(persistenceService, new SimpleMeterRegistry());
    }

    /** Persists an event without exposing repository details to messaging adapters. */
    public AuditEventDocument ingest(AuditEvent event) {
        if (event == null) {
            meterRegistry.counter("audit.events.ingestion", "outcome", "rejected").increment();
            throw new IllegalArgumentException("Audit event is required");
        }
        try {
            AuditEventDocument document = persistenceService.append(event);
            meterRegistry.counter("audit.events.ingestion", "outcome", "persisted").increment();
            return document;
        } catch (RuntimeException exception) {
            meterRegistry.counter("audit.events.ingestion", "outcome", "failed").increment();
            throw exception;
        }
    }
}
