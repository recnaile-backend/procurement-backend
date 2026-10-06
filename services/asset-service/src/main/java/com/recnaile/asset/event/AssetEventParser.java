package com.recnaile.asset.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AssetEventParser {

    private final ObjectMapper objectMapper;

    public AssetEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AssetEvent parse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Event payload must be a JSON object");
            }

            String eventId = text(root, "eventId");
            String eventType = text(root, "eventType", "type", "name");

            JsonNode payload = root.path("payload");
            if (payload.isMissingNode() || payload.isNull()) {
                payload = root.path("data");
            }
            if (payload.isMissingNode() || payload.isNull()) {
                payload = root;
            }

            if (eventId == null || eventId.isBlank()) {
                throw new IllegalArgumentException("Event ID is required");
            }
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("Event type is required");
            }

            return new AssetEvent(eventId, eventType, payload);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw new IllegalArgumentException("Invalid asset event JSON", ex);
        }
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && value.isValueNode()) {
                String result = value.asText();
                if (!result.isBlank()) {
                    return result;
                }
            }
        }
        return null;
    }
}
