package com.recnaile.notification.application.template;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTemplateCatalogTest {
    private final NotificationTemplateCatalog catalog = new NotificationTemplateCatalog();

    @Test void providesTemplatesForSupportedDomainEvents() {
        assertTrue(catalog.find("INDENT_SUBMITTED").isPresent());
        assertTrue(catalog.find("GOODS_RECEIVED").isPresent());
        assertTrue(catalog.find("LOW_STOCK_ALERT").isPresent());
    }

    @Test void lookupIsCaseInsensitiveAndUnknownEventsAreNotInvented() {
        assertEquals(catalog.find("INDENT_APPROVED"), catalog.find("indent_approved"));
        assertTrue(catalog.find("UNKNOWN_EVENT").isEmpty());
        assertTrue(catalog.find(null).isEmpty());
    }
}
