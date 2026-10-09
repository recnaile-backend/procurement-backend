package com.recnaile.returns.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class KafkaReturnEventPublisherTest {

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Test
    void publishReturnRecordedSendsJsonEventToConfiguredTopic() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaReturnEventPublisher publisher =
                new KafkaReturnEventPublisher(kafkaTemplate, objectMapper());

        ReturnRequest request = new ReturnRequest(
                "OUT%2F26-27%2F001", "ITEM-1", 1,
                "USER-1", ReturnCondition.GOOD, "ok",
                Instant.parse("2026-09-30T06:00:00Z"));
        request.setReturnId("RETURN%2F26-27%2F001");
        request.setStatus(ReturnStatus.ACCEPTED_GOOD);
        request.setUpdatedAt(Instant.parse("2026-09-30T06:05:00Z"));

        publisher.publishReturnRecorded("return.recorded", request);

        verify(kafkaTemplate).send(
                eq("return.recorded"),
                eq("RETURN%2F26-27%2F001"),
                argThat(json -> {
                    try {
                        var root = objectMapper().readTree(json);
                        return "RETURN_RECORDED".equals(root.get("eventType").asText())
                                && "RETURN%2F26-27%2F001".equals(root.get("returnId").asText())
                                && "OUT%2F26-27%2F001".equals(root.get("outwardId").asText());
                    } catch (Exception e) {
                        return false;
                    }
                }));
    }
}
