package com.recnaile.asset.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AssetEventParserTest {

    private final AssetEventParser parser = new AssetEventParser(new ObjectMapper());

    @Test
    void parsesEnvelopePayload() {
        AssetEvent event = parser.parse("""
                {
                  "eventId":"evt-001",
                  "eventType":"StockIssueApproved",
                  "payload":{"outwardId":"OUT-001","itemId":"ITEM-001"}
                }
                """);

        assertEquals("evt-001", event.eventId());
        assertEquals("StockIssueApproved", event.eventType());
        assertEquals("OUT-001", event.payload().get("outwardId").asText());
    }

    @Test
    void parsesDataEnvelope() {
        AssetEvent event = parser.parse("""
                {
                  "eventId":"evt-002",
                  "type":"ReturnAccepted",
                  "data":{"assetId":"ASSET%2F26-27%2F001"}
                }
                """);

        assertEquals("ReturnAccepted", event.eventType());
        assertEquals("ASSET%2F26-27%2F001", event.payload().get("assetId").asText());
    }

    @Test
    void rejectsMissingEventId() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse("{\"eventType\":\"StockIssueApproved\",\"payload\":{}}"));
    }
}
