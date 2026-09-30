package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaReturnEventPublisherTest {

    @Test
    void publishSendsReturnRecordedEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, ReturnRecordedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        Environment environment = mock(Environment.class);
        org.mockito.Mockito.when(environment.getProperty(
                eq("app.kafka.topics.return-recorded"), eq("return.recorded")))
                .thenReturn("return.recorded.test");

        KafkaReturnEventPublisher publisher = new KafkaReturnEventPublisher(kafkaTemplate, environment);
        ReturnRequest request = new ReturnRequest(
                "OUT-1", "LINE-1", "ITEM-1", new BigDecimal("2"),
                "USER-1", ReturnCondition.GOOD, "ok", null,
                Instant.parse("2026-09-30T06:00:00Z"));
        request.setReturnId("RET-1");
        request.setStatus(com.recnaile.returns.model.enums.ReturnStatus.ACCEPTED_GOOD);
        request.setUpdatedAt(Instant.parse("2026-09-30T06:05:00Z"));

        publisher.publish(request);

        verify(kafkaTemplate).send(eq("return.recorded.test"), eq("RET-1"), org.mockito.ArgumentMatchers.argThat(
                event -> "RETURN_RECORDED".equals(event.getEventType())
                        && "RET-1".equals(event.getReturnId())
                        && "ITEM-1".equals(event.getItemId())
                        && new BigDecimal("2").compareTo(event.getQty()) == 0
                        && ReturnCondition.GOOD == event.getCondition()
                        && Instant.parse("2026-09-30T06:05:00Z").equals(event.getOccurredAt())));
    }
}
