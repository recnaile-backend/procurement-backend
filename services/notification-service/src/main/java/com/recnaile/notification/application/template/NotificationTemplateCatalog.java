package com.recnaile.notification.application.template;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/** Version-one, deterministic templates. Keep wording free of confidential payload data. */
@Component
public class NotificationTemplateCatalog {
    private static final Map<String, NotificationTemplate> TEMPLATES = Map.ofEntries(
        Map.entry("INDENT_SUBMITTED", new NotificationTemplate("Indent submitted", "An indent has been submitted for review.")),
        Map.entry("INDENT_VERIFIED", new NotificationTemplate("Indent verified", "Your indent has been verified.")),
        Map.entry("INDENT_APPROVED", new NotificationTemplate("Indent approved", "Your indent has been approved.")),
        Map.entry("INDENT_REJECTED", new NotificationTemplate("Indent rejected", "Your indent was rejected. Review the request details.")),
        Map.entry("PO_CREATED", new NotificationTemplate("Purchase order created", "A purchase order has been created.")),
        Map.entry("PO_ISSUED", new NotificationTemplate("Purchase order issued", "A purchase order has been issued to the vendor.")),
        Map.entry("PO_CLOSED", new NotificationTemplate("Purchase order closed", "A purchase order has been closed.")),
        Map.entry("GOODS_RECEIVED", new NotificationTemplate("Goods received", "Goods have been received and recorded.")),
        Map.entry("OUTWARD_ISSUED", new NotificationTemplate("Material issued", "An approved outward request has been issued.")),
        Map.entry("RETURN_RECORDED", new NotificationTemplate("Return recorded", "A material return has been recorded.")),
        Map.entry("LOW_STOCK_ALERT", new NotificationTemplate("Low stock alert", "An inventory item has reached its configured low-stock threshold.")),
        Map.entry("SLA_BREACH", new NotificationTemplate("SLA breach alert", "A procurement workflow item has exceeded its SLA.")),
        Map.entry("REJECTION_FLAGGED", new NotificationTemplate("Rejection flagged", "A rejection requires attention."))
    );

    public Optional<NotificationTemplate> find(String eventType) {
        if (eventType == null) return Optional.empty();
        return Optional.ofNullable(TEMPLATES.get(eventType.trim().toUpperCase(java.util.Locale.ROOT)));
    }

    public Map<String, NotificationTemplate> templates() { return TEMPLATES; }
}
