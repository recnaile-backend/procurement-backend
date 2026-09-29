package com.recnaile.outward.outbox;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuditEventServiceTest {

    @Test
    void recordsAuditEventWithActorRoleAndStateChange() {
        TransactionalOutboxService outbox = mock(TransactionalOutboxService.class);
        AuditEventService service = new AuditEventService(outbox,
                Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC));
        Authentication auth = new UsernamePasswordAuthenticationToken("TL-1", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_TL")));

        service.record("OUTWARD_REQUEST", "OUT%2F26-27%2F001", "APPROVE", auth, "PENDING_APPROVAL", "APPROVED", null);

        var captor = org.mockito.ArgumentCaptor.forClass(AuditEventPayload.class);
        verify(outbox).enqueueAudit(captor.capture());
        AuditEventPayload captured = captor.getValue();
        assertEquals("AUDIT_EVENT", captured.eventType());
        assertEquals("APPROVE", captured.action());
        assertEquals("TL-1", captured.actorId());
        assertEquals("ROLE_TL", captured.actorRole());
        assertEquals("PENDING_APPROVAL", captured.beforeState());
        assertEquals("APPROVED", captured.afterState());
    }
}
