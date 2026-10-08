package com.recnaile.inventory.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inventory.service.InventoryEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final ObjectMapper objectMapper;
    private final InventoryEventService service;

    public InventoryEventListener(ObjectMapper objectMapper, InventoryEventService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @KafkaListener(
            topicPattern = "${app.kafka.events-topic-pattern:inward\\.events|outward\\.events|return\\.recorded}",
            groupId = "${spring.kafka.consumer.group-id:${spring.application.name}}")
    public void onMessage(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        try {
            service.consume(event);
        } catch (DuplicateKeyException duplicate) {
            // A concurrent/redelivered event can lose the unique movement/index
            // race after the first transaction commits. The winning transaction
            // already applied the stock change, so this delivery is acknowledged.
            log.info("Ignoring duplicate inventory event eventId={} eventType={}",
                    event.path("eventId").asText("unknown"),
                    event.path("eventType").asText("unknown"));
        }
    }
}
