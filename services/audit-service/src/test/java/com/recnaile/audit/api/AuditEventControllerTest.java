package com.recnaile.audit.api;

import com.recnaile.audit.application.AuditEventQueryService;
import com.recnaile.audit.persistence.AuditEventDocument;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditEventControllerTest {
    @Test
    void missingEventReturnsNotFoundEnvelope() {
        AuditEventQueryService service = mock(AuditEventQueryService.class);
        when(service.findByEventId(any(UUID.class))).thenReturn(Optional.empty());
        var response = new AuditEventController(service).getById(UUID.randomUUID());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("NOT_FOUND", response.getBody().error().code());
    }

    @Test
    void entityHistoryUsesBoundedDescendingPagination() {
        AuditEventQueryService service = mock(AuditEventQueryService.class);
        when(service.findEntityHistory(eq("INDENT"), eq("IND-1"), any()))
                .thenReturn(new PageImpl<>(List.<AuditEventDocument>of(), PageRequest.of(1, 10), 0));
        var response = new AuditEventController(service).entityHistory("INDENT", "IND-1", 1, 10);
        assertTrue(response.success());
        assertEquals(1, response.data().page());
        assertEquals(10, response.data().size());
        verify(service).findEntityHistory(eq("INDENT"), eq("IND-1"), argThat(pageable ->
                pageable.getPageNumber() == 1 && pageable.getPageSize() == 10
                        && pageable.getSort().getOrderFor("timestamp").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void invalidPageSizeIsRejectedBeforeQuery() {
        AuditEventQueryService service = mock(AuditEventQueryService.class);
        var controller = new AuditEventController(service);
        assertThrows(IllegalArgumentException.class, () -> controller.actorHistory("user-1", 0, 101));
        assertThrows(IllegalArgumentException.class, () -> controller.actorHistory("user-1", -1, 20));
        verifyNoInteractions(service);
    }
}
