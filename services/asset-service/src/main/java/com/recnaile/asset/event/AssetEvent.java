package com.recnaile.asset.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parsed business event envelope. The consumer accepts either a top-level
 * payload/data object or a flat event body so it remains compatible with the
 * platform envelope variants.
 */
public record AssetEvent(String eventId, String eventType, JsonNode payload) {
}
