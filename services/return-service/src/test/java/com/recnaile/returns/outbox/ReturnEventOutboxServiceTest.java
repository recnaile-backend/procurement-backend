package com.recnaile.returns.outbox;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnEventOutboxServiceTest {

    @Mock
    private ReturnEventOutboxRepository repository;

    @Mock
    private Environment environment;

    @Test
    void enqueueStoresPendingReturnRecordedEvent() {
        when(environment.getProperty("app.kafka.topics.return-recorded", "return.recorded"))
                .thenReturn("return.recorded.test");

        Instant now = Instant.parse("2026-09-30T06:05:00Z");
        ReturnRequest request = new ReturnRequest(
                "OUT-1", "ITEM-1", 2,
                "USER-1", ReturnCondition.GOOD, "ok",
                Instant.parse("2026-09-30T05:00:00Z"));
        request.setReturnId("RET-1");
        request.setStatus(ReturnStatus.ACCEPTED_GOOD);
        request.setUpdatedAt(now);

        ReturnEventOutboxService service = new ReturnEventOutboxService(
                repository, environment, Clock.fixed(now, ZoneOffset.UTC));
        when(repository.save(any(ReturnEventOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnEventOutbox result = service.enqueue(request);

        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getAggregateId()).isEqualTo("RET-1");
        assertThat(result.getEventType()).isEqualTo("RETURN_RECORDED");
        assertThat(result.getTopic()).isEqualTo("return.recorded.test");
        assertThat(result.getItemId()).isEqualTo("ITEM-1");
        verify(repository).save(any(ReturnEventOutbox.class));
    }
}
