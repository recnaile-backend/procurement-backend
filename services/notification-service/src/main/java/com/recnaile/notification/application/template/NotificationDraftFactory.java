package com.recnaile.notification.application.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.model.NotificationDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Creates persisted-notification candidates only for explicit recipient identifiers in an event payload. */
@Component
public class NotificationDraftFactory {
    private static final List<String> RECIPIENT_FIELDS = List.of("empId");
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
        Map<String, Recipient> recipients = resolveExplicitRecipients(payload);
        List<NotificationDocument> drafts = new ArrayList<>();
        for (Recipient recipient : recipients.values()) {
            drafts.add(new NotificationDocument(eventId, eventType.toUpperCase(Locale.ROOT), recipient.id(), null,
                    NotificationChannel.IN_APP, template.title(), template.message(), correlationId, traceId));
        }
        return List.copyOf(drafts);
    }

    private Map<String, Recipient> resolveExplicitRecipients(JsonNode payload) {
        // Only explicit empId values are accepted; other identity aliases and role-based targeting are deliberately ignored.
        Map<String, Recipient> recipients = new LinkedHashMap<>();
        for (String field : RECIPIENT_FIELDS) {
            JsonNode value = payload.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                addRecipient(recipients, value.asText(), null);
                break;
            }
        }
        JsonNode recipientArray = payload.get("recipients");
        if (recipientArray != null && recipientArray.isArray()) {
            for (JsonNode item : recipientArray) {
                if (item == null || !item.isObject()) continue;
                String id = text(item, "empId");
                if (id != null) addRecipient(recipients, id, null);
            }
        }
        return recipients;
    }

    private void addRecipient(Map<String, Recipient> recipients, String id, String role) {
        String normalizedId = id == null ? null : id.trim();
        if (normalizedId == null || normalizedId.isBlank()) return;
        String normalizedRole = role == null || role.isBlank() ? null : role.trim().toUpperCase(Locale.ROOT);
        recipients.putIfAbsent(normalizedId, new Recipient(normalizedId, normalizedRole));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private record Recipient(String id, String role) { }

}
