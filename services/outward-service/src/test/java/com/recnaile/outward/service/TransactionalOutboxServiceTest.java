package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recnaile.outward.domain.model.OutwardIssueTransaction;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.domain.model.OutwardItem;
import com.recnaile.outward.outbox.OutboxEvent;
import com.recnaile.outward.outbox.OutboxEventRepository;
import com.recnaile.outward.outbox.OutboxEventStatus;
import com.recnaile.outward.outbox.TransactionalOutboxService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionalOutboxServiceTest {

    @Mock OutboxEventRepository repository;

    @Test
    void enqueueCreatesPendingStockIssueApprovedEventPerItem() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        TransactionalOutboxService service = new TransactionalOutboxService(repository, objectMapper);
        Instant approvedAt = Instant.parse("2026-09-29T04:00:00Z");
        OutwardRequest request = new OutwardRequest();
        request.setOutwardNumber("OUT%2F26-27%2F001");
        request.setRequestedBy("employee-1");
        OutwardItem item = new OutwardItem("item-1", BigDecimal.valueOf(2), true);
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent event = service.enqueueStockIssueApproved(request, item, "TL-1", approvedAt);

        assertEquals("StockIssueApproved", event.getEventType());
        assertEquals("OUTWARD_REQUEST", event.getAggregateType());
        assertEquals(request.getOutwardNumber(), event.getAggregateId());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertEquals(event.getEventId(), payload.get("eventId").asText());
        assertEquals("StockIssueApproved", payload.get("eventType").asText());
        assertEquals(request.getOutwardNumber(), payload.get("outwardId").asText());
        assertEquals("item-1", payload.get("itemId").asText());
        assertEquals(2, payload.get("qty").asInt());
        assertEquals("employee-1", payload.get("userId").asText());
        assertEquals("TL-1", payload.get("approvedBy").asText());
        assertTrue(payload.get("reusable").asBoolean());
    }

    @Test
    void enqueueCreatesPendingOutwardIssuedEvent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        TransactionalOutboxService service = new TransactionalOutboxService(repository, objectMapper);
        Instant issuedAt = Instant.parse("2026-09-29T04:00:00Z");
        OutwardIssueTransaction transaction = new OutwardIssueTransaction();
        transaction.setOutwardNumber("OUT%2F26-27%2F001");
        transaction.setIssuedAt(issuedAt);
        transaction.setRequestedBy("employee-1");
        transaction.setIssuerId("issuer-1");
        transaction.setApproverId("approver-1");
        transaction.setDepartmentName("Engineering");
        transaction.setProjectId("project-1");
        transaction.setRecipientEmployeeId("employee-2");
        transaction.setRecipientEmployeeName("Employee Two");
        transaction.setPurpose("Project requirement");
        transaction.setAssetTracked(true);
        transaction.setItems(List.of(new OutwardItem("item-1", BigDecimal.ONE)));
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent event = service.enqueueOutwardIssued(transaction);

        assertEquals("OUTWARD_ISSUED", event.getEventType());
        assertEquals("OUTWARD_REQUEST", event.getAggregateType());
        assertEquals(transaction.getOutwardNumber(), event.getAggregateId());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttemptCount());
        assertTrue(event.getEventId() != null && !event.getEventId().isBlank());

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertEquals(event.getEventId(), payload.get("eventId").asText());
        assertEquals("OUTWARD_ISSUED", payload.get("eventType").asText());
        assertEquals(1, payload.get("eventVersion").asInt());
        assertEquals(transaction.getOutwardNumber(), payload.get("outwardNumber").asText());
        assertEquals("item-1", payload.get("items").get(0).get("itemId").asText());
    }
}
