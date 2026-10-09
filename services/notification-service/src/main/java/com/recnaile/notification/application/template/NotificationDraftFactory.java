package com.recnaile.notification.application.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.model.NotificationDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Creates persisted-notification candidates only for explicit recipient identifiers in an event payload. */
@Component
public class NotificationDraftFactory {
    private static final List<String> RECIPIENT_FIELDS = List.of("recipientId", "requestedBy", "createdBy", "employeeId", "userId");
    private final NotificationTemplateCatalog catalog;

    public NotificationDraftFactory(NotificationTemplateCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public List<NotificationDocument> createInAppDrafts(JsonNode envelope, String correlationId, String traceId) {
        if (envelope == null || !envelope.isObject()) throw new IllegalArgumentException("INVALID_EVENT_ENVELOPE");
        String eventId = text(envelope, "eventId");
        String eventType = text(envelope, "eventType");
        JsonNode payload = envelope.path("payload");
        if (eventId == null || eventType == null || !payload.isObject())
            throw new IllegalArgumentException("INVALID_EVENT_ENVELOPE");
        NotificationTemplate template = catalog.find(eventType)
                .orElseThrow(() -> new IllegalArgumentException("UNSUPPORTED_EVENT_TYPE"));
        List<String> recipients = resolveExplicitRecipients(payload);
        List<NotificationDocument> drafts = new ArrayList<>();
        for (String recipient : recipients) {
            drafts.add(new NotificationDocument(eventId, eventType.toUpperCase(Locale.ROOT), recipient, null,
                    NotificationChannel.IN_APP, template.title(), template.message(), correlationId, traceId));
        }
        return List.copyOf(drafts);
    }

    private List<String> resolveExplicitRecipients(JsonNode payload) {
        // Deliberately do not infer users from raw HTTP headers or untrusted role claims.
        for (String field : RECIPIENT_FIELDS) {
            JsonNode value = payload.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank())
                return List.of(value.asText().trim());
        }
        return List.of();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }
}
