package com.recnaile.notification.application.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.notification.domain.enums.NotificationChannel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationDraftFactoryTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final NotificationDraftFactory factory = new NotificationDraftFactory(new NotificationTemplateCatalog());

    @Test void createsOneInAppDraftForAnExplicitPayloadRecipient() throws Exception {
        var event = mapper.readTree("""
            {"eventId":"evt-1","eventType":"INDENT_SUBMITTED","payload":{"requestedBy":"user-17"}}
            """);
        var drafts = factory.createInAppDrafts(event, "corr-1", "trace-1");
        assertEquals(1, drafts.size());
        assertEquals("user-17", drafts.get(0).getRecipientId());
        assertEquals(NotificationChannel.IN_APP, drafts.get(0).getChannel());
        assertEquals("corr-1", drafts.get(0).getCorrelationId());
    }

    @Test void doesNotGuessRecipientsWhenPayloadHasNoExplicitUser() throws Exception {
        var event = mapper.readTree("""
            {"eventId":"evt-2","eventType":"INDENT_APPROVED","payload":{"departmentId":"dept-4"}}
            """);
        assertTrue(factory.createInAppDrafts(event, null, null).isEmpty());
    }

    @Test void createsDeduplicatedDraftsForExplicitRecipientArrayAndPreservesRole() throws Exception {
        var event = mapper.readTree("""
            {"eventId":"evt-4","eventType":"SLA_BREACH","payload":{"recipients":[
              {"recipientId":"manager-1","recipientRole":"manager"},
              {"userId":"manager-1","role":"MANAGER"},
              {"recipientId":"procurement-2","recipientRole":"procurement"},
              {"recipientRole":"missing-id"}, null, "not-an-object"
            ]}}
            """);
        var drafts = factory.createInAppDrafts(event, null, null);
        assertEquals(2, drafts.size());
        assertEquals("manager-1", drafts.get(0).getRecipientId());
        assertEquals("MANAGER", drafts.get(0).getRecipientRole());
        assertEquals("procurement-2", drafts.get(1).getRecipientId());
        assertEquals("PROCUREMENT", drafts.get(1).getRecipientRole());
    }

    @Test void rejectsMalformedEnvelopeAndUnknownEventType() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> factory.createInAppDrafts(mapper.readTree("{}"), null, null));
        var event = mapper.readTree("""
            {"eventId":"evt-3","eventType":"UNKNOWN_EVENT","payload":{"userId":"u1"}}
            """);
        assertThrows(IllegalArgumentException.class, () -> factory.createInAppDrafts(event, null, null));
    }
}
