package com.recnaile.returns.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.OutboxStatus;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnEventOutboxServiceTest {

    @Mock
    private ReturnEventOutboxRepository repository;

    @Mock
    private Environment environment;

    @Test
    void enqueueStoresInventoryReturnRecordedEventWithOutwardIdentity() throws Exception {
        when(environment.getProperty("app.kafka.topics.return-recorded", "return.recorded"))
                .thenReturn("return.recorded.test");

        Instant now = Instant.parse("2026-09-30T06:05:00Z");
        ReturnRequest request = returnRequest(false);

        ReturnEventOutboxService service = service(now);
        when(repository.save(any(ReturnEventOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnEventOutbox result = service.enqueue(request);

        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getAggregateId()).isEqualTo("RETURN%2F26-27%2F001");
        assertThat(result.getEventType()).isEqualTo("RETURN_RECORDED");
        assertThat(result.getTopic()).isEqualTo("return.recorded.test");
        assertThat(result.getOutwardId()).isEqualTo("OUT%2F26-27%2F001");
        assertThat(objectMapper().readTree(result.getPayload()).get("outwardId").asText())
                .isEqualTo("OUT%2F26-27%2F001");
        verify(repository).save(any(ReturnEventOutbox.class));
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private ReturnEventOutboxService service(Instant now) {
        return new ReturnEventOutboxService(
                repository,
                objectMapper(),
                environment,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private ReturnRequest returnRequest(boolean assetTracked) {
        ReturnRequest request = new ReturnRequest(
                "OUT%2F26-27%2F001",
                "ITEM-1",
                1,
                "USER-1",
                ReturnCondition.GOOD,
                "ok",
                Instant.parse("2026-09-30T05:00:00Z"),
                assetTracked);
        request.setReturnId("RETURN%2F26-27%2F001");
        request.setStatus(ReturnStatus.ACCEPTED_GOOD);
        request.setUpdatedAt(Instant.parse("2026-09-30T06:00:00Z"));
        return request;
    }
}
