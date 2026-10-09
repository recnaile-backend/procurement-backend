package com.recnaile.notification.application.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.notification.domain.enums.NotificationChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDraftFactoryTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final NotificationDraftFactory factory = new NotificationDraftFactory(new NotificationTemplateCatalog());

    @Test
    void createsOneInAppDraftForAnExplicitEmpId() throws Exception {
        var event = mapper.readTree("""
            {"eventId":"evt-1","eventType":"INDENT_SUBMITTED","payload":{"empId":"EMP-17"}}
            """);
        var drafts = factory.createInAppDrafts(event, "corr-1", "trace-1");
        assertEquals(1, drafts.size());
        assertEquals("EMP-17", drafts.get(0).getEmpId());
        assertEquals(NotificationChannel.IN_APP, drafts.get(0).getChannel());
        assertEquals("corr-1", drafts.get(0).getCorrelationId());
    }

    @Test
    void doesNotGuessRecipientsFromLegacyIdentityAliases() throws Exception {
        var event = mapper.readTree("""
            {"eventId":"evt-2","eventType":"INDENT_APPROVED","payload":{"requestedBy":"EMP-1","createdBy":"EMP-2","employeeId":"EMP-3","userId":"EMP-4","recipientId":"EMP-5","departmentId":"dept-4"}}
            """);
        assertTrue(factory.createInAppDrafts(event, null, null).isEmpty());
    }

    @Test
    void createsDeduplicatedDraftsOnlyForExplicitEmpIds() throws Exception {
        var event = mapper.readTree("""
            {"eventId":"evt-4","eventType":"SLA_BREACH","payload":{"recipients":[
              {"empId":"EMP-1"},
              {"empId":"EMP-1"},
              {"empId":"EMP-2"},
              {"userId":"EMP-3"},
              {"recipientId":"EMP-4"},
              {}, null, "not-an-object"
            ]}}
            """);
        var drafts = factory.createInAppDrafts(event, null, null);
        assertEquals(2, drafts.size());
        assertEquals("EMP-1", drafts.get(0).getEmpId());
        assertEquals("EMP-2", drafts.get(1).getEmpId());
        assertNull(drafts.get(0).getRecipientRole());
    }

    @Test
    void rejectsMalformedEnvelopeAndUnknownEventType() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> factory.createInAppDrafts(mapper.readTree("{}"), null, null));
        var event = mapper.readTree("""
            {"eventId":"evt-3","eventType":"UNKNOWN_EVENT","payload":{"empId":"EMP-1"}}
            """);
        assertThrows(IllegalArgumentException.class, () -> factory.createInAppDrafts(event, null, null));
    }
}
