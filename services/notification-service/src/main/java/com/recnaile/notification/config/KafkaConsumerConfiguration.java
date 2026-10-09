package com.recnaile.notification.config;

import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.CommonErrorHandler;

@Configuration
public class KafkaConsumerConfiguration {
    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler notificationCommonErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // Spring Boot 4 exposes this configurer with Object/Object generic parameters.
        // Kafka uses String keys and values here; the factory type is erased at runtime.
        @SuppressWarnings({"rawtypes", "unchecked"})
        ConcurrentKafkaListenerContainerFactory rawFactory = factory;
        @SuppressWarnings({"rawtypes", "unchecked"})
        ConsumerFactory rawConsumerFactory = consumerFactory;
        configurer.configure(rawFactory, rawConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(notificationCommonErrorHandler);
        return factory;
    }
}
