package com.recnaile.inward;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.model.GRNStatus;
import com.recnaile.inward.model.OutboxEvent;
import com.recnaile.inward.outbox.OutboxService;
import com.recnaile.inward.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxServiceTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsOneGoodsReceivedEventPerGrnLine() throws Exception {
        OutboxService service = new OutboxService(repository, objectMapper, clock, "procurement.events");

        GRNResponse grn = new GRNResponse(
                "mongo-id", "GRN%2F26-27%2F001", "PO-009", "3D", "DN-001",
                Instant.parse("2026-09-24T09:00:00Z"),
                Instant.parse("2026-09-24T10:00:00Z"), "Rajesh", GRNStatus.DRAFT,
                List.of(
                        new GRNLine("Bolt", new BigDecimal("10"), new BigDecimal("9"), BigDecimal.ONE, "PCS", "Damaged"),
                        new GRNLine("Nut", new BigDecimal("5"), new BigDecimal("5"), BigDecimal.ZERO, "PCS", null)
                ), 0L, null, null);

        when(repository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        service.enqueueGoodsReceived(grn);
        service.enqueueGRNCreateAuditEvent(grn);

        verify(repository, times(3)).save(any(OutboxEvent.class));
        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository, times(3)).save(captor.capture());

        long goodsReceivedCount = captor.getAllValues().stream()
                .filter(event -> "GOODS_RECEIVED".equals(event.getEventType()))
                .count();
        long auditCount = captor.getAllValues().stream()
                .filter(event -> "AUDIT_EVENT".equals(event.getEventType()))
                .count();

        assertEquals(2, goodsReceivedCount);
        assertEquals(1, auditCount);

        for (OutboxEvent event : captor.getAllValues()) {
            assertEquals("procurement.events", event.getTopic());
            JsonNode json = objectMapper.readTree(event.getPayload());
            assertEquals(event.getEventType(), json.get("eventType").asText());
            assertEquals(1, json.get("eventVersion").asInt());
            assertEquals("inward-service", json.get("producer").asText());

            JsonNode payload = json.get("payload");
            if ("GOODS_RECEIVED".equals(event.getEventType())) {
                assertEquals("GRN%2F26-27%2F001", payload.get("grnId").asText());
            } else if ("AUDIT_EVENT".equals(event.getEventType())) {
                assertEquals("GRN", payload.get("entityType").asText());
                assertEquals("GRN%2F26-27%2F001", payload.get("entityId").asText());
            }
        }

        OutboxEvent auditEvent = captor.getAllValues().stream()
                .filter(event -> "AUDIT_EVENT".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();
        JsonNode auditPayload = objectMapper.readTree(auditEvent.getPayload()).get("payload");
        assertEquals("GRN", auditPayload.get("entityType").asText());
        assertEquals("GRN%2F26-27%2F001", auditPayload.get("entityId").asText());
        assertEquals("GRN_CREATED", auditPayload.get("action").asText());
        assertEquals("system", auditPayload.get("actorId").asText());
    }
}
