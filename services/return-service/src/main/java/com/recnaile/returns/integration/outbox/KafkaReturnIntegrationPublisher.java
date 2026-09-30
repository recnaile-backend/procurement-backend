package com.recnaile.returns.integration.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
public class KafkaReturnIntegrationPublisher {
    private final KafkaTemplate<String,String> kafkaTemplate;
    public KafkaReturnIntegrationPublisher(KafkaTemplate<String,String> kafkaTemplate){this.kafkaTemplate=kafkaTemplate;}
    public CompletableFuture<?> publish(String topic,String key,String payload){return kafkaTemplate.send(topic,key,payload);}
}
