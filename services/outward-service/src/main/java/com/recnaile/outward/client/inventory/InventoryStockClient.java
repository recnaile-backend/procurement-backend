package com.recnaile.outward.client.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.recnaile.outward.exception.DownstreamServiceException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Synchronous stock-availability boundary. Inventory remains the source of
 * truth; Outward never reads or writes Inventory's database.
 */
@Component
public class InventoryStockClient {

    private final RestClient restClient;
    private final String baseUrl;

    public InventoryStockClient(
            RestClient restClient,
            @Value("${app.clients.inventory-base-url:http://inventory-service:8086}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Observed(name = "outward.inventory.stock", contextualName = "get-inventory-stock")
    public BigDecimal getAvailableQuantity(String itemId) {
        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl + "/api/v1/inventory/stock/{itemId}", itemId)
                    .retrieve()
                    .body(JsonNode.class);
            return extractAvailableQuantity(response, itemId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return BigDecimal.ZERO;
            }
            throw unavailable("Unable to validate inventory stock", ex);
        } catch (RestClientException ex) {
            throw unavailable("Unable to validate inventory stock", ex);
        }
    }

    private BigDecimal extractAvailableQuantity(JsonNode response, String itemId) {
        JsonNode data = response;
        if (data != null && data.isObject() && data.has("data")) {
            data = data.get("data");
        }

        JsonNode quantity = findQuantity(data);
        if (quantity == null || quantity.isNull()) {
            throw new DownstreamServiceException(
                    "INVENTORY_RESPONSE_INVALID",
                    "Inventory response does not contain available quantity for item: " + itemId,
                    null);
        }

        try {
            if (quantity.isNumber()) {
                return quantity.decimalValue();
            }
            if (quantity.isTextual()) {
                return new BigDecimal(quantity.textValue().trim());
            }
        } catch (NumberFormatException ex) {
            throw new DownstreamServiceException(
                    "INVENTORY_RESPONSE_INVALID",
                    "Inventory returned an invalid available quantity for item: " + itemId,
                    ex);
        }

        throw new DownstreamServiceException(
                "INVENTORY_RESPONSE_INVALID",
                "Inventory returned an unsupported available quantity for item: " + itemId,
                null);
    }

    private JsonNode findQuantity(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            for (String key : new String[] {"availableQuantity", "availableQty", "availableStock"}) {
                JsonNode candidate = node.get(key);
                if (candidate != null && (candidate.isNumber() || candidate.isTextual())) {
                    return candidate;
                }
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                JsonNode nested = findQuantity(fields.next().getValue());
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode nested = findQuantity(child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private DownstreamServiceException unavailable(String message, Throwable cause) {
        return new DownstreamServiceException("INVENTORY_SERVICE_UNAVAILABLE", message, cause);
    }
}
