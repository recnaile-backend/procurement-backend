package com.recnaile.indent.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.repository.IndentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SlaBreachServiceTest {

    @Mock
    private IndentRepository indentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ApprovalProperties approvalProperties;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private SlaBreachService service;

    @Test
    void overdueIndent_shouldBeClaimedAndPublishSlaBreachEvent() {
        Instant dueAt = Instant.now().minusSeconds(3_600);
        Indent candidate = indent("IND-1", dueAt);

        when(indentRepository
                .findByStatusAndApprovalDueAtLessThanEqualOrderByApprovalDueAtAsc(
                        eq(IndentStatus.PENDING_APPROVAL),
                        any(Instant.class),
                        any(Pageable.class)))
                .thenReturn(List.of(candidate));

        Indent claimed = indent("IND-1", dueAt);
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Indent.class)))
                .thenReturn(claimed);

        when(approvalProperties.getSlaDuration())
                .thenReturn(java.time.Duration.ofHours(24));

        service.detectAndPublishBreaches();

        verify(mongoTemplate).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Indent.class));

        ArgumentCaptor<Object> payloadCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(outboxEventService).create(
                eq("SLA_BREACH"),
                eq("1"),
                any(Instant.class),
                eq("IND-1"),
                eq((String) null),
                eq((String) null),
                eq((String) null),
                eq((String) null),
                eq("procurement.indent.events"),
                payloadCaptor.capture());

        String payload = payloadCaptor.getValue().toString();
        assertTrue(payload.contains("entityType=INDENT"));
        assertTrue(payload.contains("entityId=IND-1"));
        assertTrue(payload.contains("slaWindowHours=24.0000"));
    }

    @Test
    void lostClaim_shouldNotPublishSlaBreachEvent() {
        Instant dueAt = Instant.now().minusSeconds(3_600);
        Indent candidate = indent("IND-2", dueAt);

        when(indentRepository
                .findByStatusAndApprovalDueAtLessThanEqualOrderByApprovalDueAtAsc(
                        eq(IndentStatus.PENDING_APPROVAL),
                        any(Instant.class),
                        any(Pageable.class)))
                .thenReturn(List.of(candidate));

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Indent.class)))
                .thenReturn(null);

        service.detectAndPublishBreaches();

        verify(mongoTemplate).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Indent.class));

        verifyNoInteractions(outboxEventService);
    }

    private Indent indent(String id, Instant dueAt) {
        Indent indent = new Indent();
        indent.setId(id);
        indent.setStatus(IndentStatus.PENDING_APPROVAL);
        indent.setApprovalDueAt(dueAt);
        return indent;
    }
}
