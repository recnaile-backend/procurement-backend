package com.recnaile.returns.integration.outbox;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
import static org.mockito.Mockito.*;

class ReturnIntegrationOutboxDispatcherTest {
    @Test
    void successfulPublishMarksOutboxSent() throws Exception {
        ReturnIntegrationOutboxRepository repository = mock(ReturnIntegrationOutboxRepository.class);
        KafkaReturnIntegrationPublisher publisher = mock(KafkaReturnIntegrationPublisher.class);
        ReturnIntegrationOutbox outbox = new ReturnIntegrationOutbox("event-1","RET-1","RETURN_AUDIT","AUDIT","audit.events","{}",Instant.now());
        when(publisher.publish("audit.events","RET-1","{}")).thenReturn(CompletableFuture.completedFuture(null));

        ReturnIntegrationOutboxDispatcher dispatcher = new ReturnIntegrationOutboxDispatcher(repository,publisher,java.time.Clock.systemUTC());
        dispatcher.dispatchOne(outbox);

        verify(repository).save(outbox);
        org.assertj.core.api.Assertions.assertThat(outbox.getStatus()).isEqualTo(IntegrationOutboxStatus.SENT);
    }
}
