package com.recnaile.purchaseorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.service.PurchaseOrderEventPublisher;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class PurchaseOrderEventPublisherTest {

    @Test
    void publishCreated_shouldUseDocumentedEnvelopeAndTopic() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("procurement.purchase-order.events", 0),
                1L,
                0,
                System.currentTimeMillis(),
                0,
                0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        PurchaseOrderEventPublisher publisher = new PurchaseOrderEventPublisher(
                kafkaTemplate,
                objectMapper,
                "procurement.purchase-order.events");

        PurchaseOrder po = new PurchaseOrder();
        po.setPoId("PO-ID-001");
        po.setPoNumber("PO-2026-0001");
        po.setPoDate(LocalDate.of(2026, 9, 19));
        po.transitionStatus(PurchaseOrderStatus.DRAFT);

        publisher.publishCreated(po, "user-001");

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                eq("procurement.purchase-order.events"),
                eq("PO-ID-001"),
                jsonCaptor.capture());

        String json = jsonCaptor.getValue();
        assertThat(json).contains("\"eventType\":\"PO_CREATED\"");
        assertThat(json).contains("\"eventVersion\":\"1\"");
        assertThat(json).contains("\"producerService\":\"purchase-order-service\"");
        assertThat(json).contains("\"aggregateType\":\"PURCHASE_ORDER\"");
        assertThat(json).contains("\"aggregateId\":\"PO-ID-001\"");
        assertThat(json).contains("\"actorId\":\"user-001\"");
        assertThat(json).contains("\"schemaVersion\":\"1\"");
        assertThat(json).contains("\"poNumber\":\"PO-2026-0001\"");
    }
}
