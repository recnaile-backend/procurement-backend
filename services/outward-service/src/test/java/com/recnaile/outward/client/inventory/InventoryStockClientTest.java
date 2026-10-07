package com.recnaile.outward.client.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.recnaile.outward.exception.DownstreamServiceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class InventoryStockClientTest {

    @Test
    void readsAvailableQuantityFromDirectInventoryResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryStockClient client = new InventoryStockClient(builder.build(), "http://inventory-service:8086");

        server.expect(requestTo("http://inventory-service:8086/api/v1/inventory/stock/ITEM-1"))
                .andRespond(withSuccess("{\"itemId\":\"ITEM-1\",\"availableQuantity\":7}", MediaType.APPLICATION_JSON));

        assertEquals(new BigDecimal("7"), client.getAvailableQuantity("ITEM-1"));
        server.verify();
    }

    @Test
    void readsAvailableQuantityFromStandardEnvelope() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryStockClient client = new InventoryStockClient(builder.build(), "http://inventory-service:8086");

        server.expect(requestTo("http://inventory-service:8086/api/v1/inventory/stock/ITEM-1"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"availableQty\":\"3.5\"},\"error\":null}", MediaType.APPLICATION_JSON));

        assertEquals(new BigDecimal("3.5"), client.getAvailableQuantity("ITEM-1"));
        server.verify();
    }

    @Test
    void rejectsMalformedInventoryResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryStockClient client = new InventoryStockClient(builder.build(), "http://inventory-service:8086");

        server.expect(requestTo("http://inventory-service:8086/api/v1/inventory/stock/ITEM-1"))
                .andRespond(withSuccess("{\"itemId\":\"ITEM-1\"}", MediaType.APPLICATION_JSON));

        DownstreamServiceException exception = assertThrows(DownstreamServiceException.class,
                () -> client.getAvailableQuantity("ITEM-1"));
        assertEquals("INVENTORY_RESPONSE_INVALID", exception.getCode());
        server.verify();
    }
}
