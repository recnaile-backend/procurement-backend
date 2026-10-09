package com.recnaile.notification.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Bounded retry with a dead-letter topic; failed messages are not silently discarded. */
@Configuration
public class KafkaRetryConfiguration {
    @Bean
    CommonErrorHandler notificationCommonErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${app.kafka.dlt-suffix:.dlt}") String dltSuffix,
            @Value("${app.kafka.retry-interval-ms:1000}") long retryIntervalMs,
            @Value("${app.kafka.retry-attempts:3}") long retryAttempts) {
        if (dltSuffix == null || dltSuffix.isBlank() || retryIntervalMs < 0 || retryAttempts < 0) {
            throw new IllegalArgumentException("Invalid Kafka retry/DLT configuration");
        }
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(record.topic() + dltSuffix, 0));
        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer, new FixedBackOff(retryIntervalMs, retryAttempts));
        // Invalid envelopes are deterministic; route them directly to the DLT instead of retrying.
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
