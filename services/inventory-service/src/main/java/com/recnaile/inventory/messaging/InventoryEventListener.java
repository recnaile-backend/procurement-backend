package com.recnaile.inventory.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inventory.service.InventoryEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {
    private final ObjectMapper objectMapper; private final InventoryEventService service;
    public InventoryEventListener(ObjectMapper objectMapper,InventoryEventService service){this.objectMapper=objectMapper;this.service=service;}

    @KafkaListener(topics="${app.kafka.events-topic:procurement.events}", groupId="${spring.application.name}")
    public void onMessage(String message) throws Exception {
        JsonNode event=objectMapper.readTree(message);
        service.consume(event);
    }
}
