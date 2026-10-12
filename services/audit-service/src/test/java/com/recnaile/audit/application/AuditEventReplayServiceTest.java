package com.recnaile.audit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuditEventReplayServiceTest {
    @Test
    void rejectsMalformedJsonWithoutPublishing() {
        @SuppressWarnings("unchecked") KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var service = new AuditEventReplayService(new ObjectMapper(), validatorFactory.getValidator(), template,
                    "procurement.audit.events");
            assertThrows(IllegalArgumentException.class, () -> service.replay("{not-json"));
            verifyNoInteractions(template);
        } finally {
            validatorFactory.close();
        }
    }

    @Test
    void rejectsBlankPayloadWithoutPublishing() {
        @SuppressWarnings("unchecked") KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            var service = new AuditEventReplayService(new ObjectMapper(), validatorFactory.getValidator(), template,
                    "procurement.audit.events");
            assertThrows(IllegalArgumentException.class, () -> service.replay("  "));
            verifyNoInteractions(template);
        } finally {
            validatorFactory.close();
        }
    }
}
