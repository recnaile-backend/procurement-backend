package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
        ReturnRecordedEvent event = new ReturnRecordedEvent(
                returnRequest.getReturnId(),
                returnRequest.getItemId(),
                returnRequest.getQuantity(),
                returnRequest.getCondition(),
                returnRequest.getUpdatedAt());
        kafkaTemplate.send(topic, returnRequest.getReturnId(), event);
    }
}
