package com.recnaile.returns.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReturnIntegrationOutboxServiceTest {

    private static ReturnIntegrationOutboxService objectMapperService(ReturnIntegrationOutboxRepository repository) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new ReturnIntegrationOutboxService(repository, objectMapper, new MockEnvironment());
    }
    @Test
    void enqueueAuditAndNotificationCreatesPendingRecords() {
        ReturnIntegrationOutboxRepository repository = mock(ReturnIntegrationOutboxRepository.class);
        when(repository.save(any(ReturnIntegrationOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReturnIntegrationOutboxService service = objectMapperService(repository);

        service.audit("RET-1", "user-1", "PROCUREMENT", "VERIFY", "PENDING_VERIFICATION", "VERIFIED", "corr-1");
        service.notify("RET-1", "user-1", "VERIFY", "Return verified", "corr-1");

        verify(repository, times(2)).save(any(ReturnIntegrationOutbox.class));
        assertThat(repository).isNotNull();
    }
}
