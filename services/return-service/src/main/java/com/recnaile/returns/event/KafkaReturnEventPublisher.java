package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaReturnEventPublisher implements ReturnEventPublisher {

    private final KafkaTemplate<String, ReturnRecordedEvent> kafkaTemplate;
    private final String topic;

    public KafkaReturnEventPublisher(
            KafkaTemplate<String, ReturnRecordedEvent> kafkaTemplate,
            org.springframework.core.env.Environment environment) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = environment.getProperty("app.kafka.topics.return-recorded", "return.recorded");
    }

    @Override
    public void publish(ReturnRequest returnRequest) {
        publish(topic, returnRequest.getReturnId(), ReturnRecordedEvent.create(returnRequest));
    }

    public CompletableFuture<?> publish(String eventTopic, String key, ReturnRecordedEvent event) {
        return kafkaTemplate.send(eventTopic, key, event);
    }
}
