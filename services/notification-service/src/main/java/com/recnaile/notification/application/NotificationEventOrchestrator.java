package com.recnaile.notification.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.recnaile.notification.application.template.NotificationDraftFactory;
import com.recnaile.notification.domain.model.IncomingDomainEvent;
import com.recnaile.notification.domain.model.NotificationDocument;
import com.recnaile.notification.persistence.repository.IncomingDomainEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Converts a durable event-inbox entry into idempotently persisted in-app notifications. */
@Service
public class NotificationEventOrchestrator {
    private final NotificationDraftFactory draftFactory;
    private final NotificationPersistenceService notificationPersistence;
    private final IncomingDomainEventRepository inboxRepository;
    private final EventIdempotencyService idempotencyService;

    public NotificationEventOrchestrator(NotificationDraftFactory draftFactory,
            NotificationPersistenceService notificationPersistence,
            IncomingDomainEventRepository inboxRepository,
            EventIdempotencyService idempotencyService) {
        this.draftFactory = Objects.requireNonNull(draftFactory, "draftFactory");
        this.notificationPersistence = Objects.requireNonNull(notificationPersistence, "notificationPersistence");
        this.inboxRepository = Objects.requireNonNull(inboxRepository, "inboxRepository");
        this.idempotencyService = Objects.requireNonNull(idempotencyService, "idempotencyService");
    }

    /**
     * Processing is safe to repeat after a crash: notification uniqueness is enforced by MongoDB.
     * The caller must acknowledge Kafka only after this method returns successfully.
     */
    public ProcessingResult process(JsonNode envelope, String correlationId, String traceId) {
        if (envelope == null || !envelope.isObject()) throw new IllegalArgumentException("INVALID_EVENT_ENVELOPE");
        String eventId = text(envelope, "eventId");
        if (eventId == null) throw new IllegalArgumentException("INVALID_EVENT_ENVELOPE");
        IncomingDomainEvent inbox = inboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("EVENT_NOT_IN_INBOX"));
        inbox.markProcessing();
        inboxRepository.save(inbox);
        try {
            List<NotificationDocument> drafts = draftFactory.createInAppDrafts(envelope, correlationId, traceId);
            for (NotificationDocument draft : drafts) notificationPersistence.saveIfAbsent(draft);
            inbox.markProcessed();
            inboxRepository.save(inbox);
            idempotencyService.markProcessed(eventId, Instant.now());
            return new ProcessingResult(eventId, drafts.size(), true);
        } catch (RuntimeException failure) {
            inbox.markFailed();
            inboxRepository.save(inbox);
            idempotencyService.markFailed(eventId, safeFailureCode(failure), Instant.now());
            throw failure;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private String safeFailureCode(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) return "NOTIFICATION_PROCESSING_FAILED";
        // Persist only stable, bounded error codes; never persist exception details or payload data.
        String normalized = message.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    public record ProcessingResult(String eventId, int notificationDraftCount, boolean processed) { }
}
