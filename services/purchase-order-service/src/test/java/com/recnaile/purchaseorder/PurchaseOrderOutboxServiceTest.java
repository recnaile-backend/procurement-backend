package com.recnaile.purchaseorder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.OutboxEvent;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.repository.OutboxEventRepository;
import com.recnaile.purchaseorder.service.PurchaseOrderOutboxService;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PurchaseOrderOutboxServiceTest {

    @Test
    void enqueueCreated_shouldPersistDocumentedEnvelope() throws Exception {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        Tracer tracer = mock(Tracer.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        PurchaseOrderOutboxService service = new PurchaseOrderOutboxService(
                repository,
                objectMapper,
                tracer,
                "procurement.purchase-order.events");

        PurchaseOrder po = new PurchaseOrder();
        po.setPoId("PO-ID-001");
        po.setPoNumber("PO-2026-0001");
        po.setPoDate(LocalDate.of(2026, 9, 19));
        po.transitionStatus(PurchaseOrderStatus.DRAFT);

        service.enqueueCreated(po, "user-001");

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("PO_CREATED");
        assertThat(event.getEventVersion()).isEqualTo("1");
        assertThat(event.getProducerService()).isEqualTo("purchase-order-service");
        assertThat(event.getAggregateType()).isEqualTo("PURCHASE_ORDER");
        assertThat(event.getAggregateId()).isEqualTo("PO-ID-001");
        assertThat(event.getTopic()).isEqualTo("procurement.purchase-order.events");
        assertThat(event.getStatus()).isEqualTo(com.recnaile.purchaseorder.domain.enums.OutboxStatus.PENDING);
        assertThat(event.getAttemptCount()).isZero();

        JsonNode envelope = objectMapper.readTree(event.getPayload());
        assertThat(envelope.get("eventId").asText()).isEqualTo(event.getEventId());
        assertThat(envelope.get("eventType").asText()).isEqualTo("PO_CREATED");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo("PO-ID-001");
        assertThat(envelope.get("actorId").asText()).isEqualTo("user-001");
        assertThat(envelope.get("schemaVersion").asText()).isEqualTo("1");
        assertThat(envelope.get("payload").get("poNumber").asText()).isEqualTo("PO-2026-0001");
    }
}
