package com.recnaile.outward.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class NotificationEventServiceTest {

    @Test
    void approvalCreatesRequesterNotificationHook() {
        TransactionalOutboxService outbox = mock(TransactionalOutboxService.class);
        NotificationEventService service = new NotificationEventService(outbox,
                Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC));
        Authentication auth = new UsernamePasswordAuthenticationToken("TL-1", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_TL")));

        service.notifyApproval("OUT%2F26-27%2F001", "EMP-1", auth);

        var captor = org.mockito.ArgumentCaptor.forClass(NotificationEventPayload.class);
        verify(outbox).enqueueNotification(captor.capture());
        NotificationEventPayload captured = captor.getValue();
        assertEquals(NotificationEventService.APPROVAL, captured.eventType());
        assertEquals(NotificationEventService.APPROVAL, captured.notificationType());
        assertEquals("EMP-1", captured.recipientId());
        assertEquals("OUT%2F26-27%2F001", captured.entityId());
    }

    @Test
    void rejectionIncludesReasonInNotification() {
        TransactionalOutboxService outbox = mock(TransactionalOutboxService.class);
        NotificationEventService service = new NotificationEventService(outbox, Clock.systemUTC());
        Authentication auth = new UsernamePasswordAuthenticationToken("MD-1", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_MD")));

        service.notifyRejection("OUT%2F26-27%2F002", "EMP-2", "Insufficient justification", auth);

        var captor = org.mockito.ArgumentCaptor.forClass(NotificationEventPayload.class);
        verify(outbox).enqueueNotification(captor.capture());
        assertEquals(NotificationEventService.REJECTION, captor.getValue().notificationType());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().message().contains("Insufficient justification"));
    }
}
