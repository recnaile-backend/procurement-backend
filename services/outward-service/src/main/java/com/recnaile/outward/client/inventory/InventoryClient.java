package com.recnaile.outward.client.inventory;

import com.recnaile.outward.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InventoryClient {

    private final RestClient restClient;
    private final String baseUrl;

    public InventoryClient(RestClient restClient,
                           @Value("${app.clients.inventory-base-url:http://inventory-service:8086}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public InventorySummaryResponse summary(String itemId) {
        try {
            InventorySummaryResponse response = restClient.get()
                    .uri(baseUrl + "/api/v1/inventory/stock/{itemId}/summary", itemId)
                    .retrieve()
                    .body(InventorySummaryResponse.class);
            if (response == null) {
                throw new DownstreamServiceException("INVENTORY_UNAVAILABLE",
                        "Inventory service returned an empty response");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new DownstreamServiceException("INVENTORY_UNAVAILABLE",
                        "Inventory summary was not found for item " + itemId, ex);
            }
            throw new DownstreamServiceException("INVENTORY_SERVICE_UNAVAILABLE",
                    "Unable to validate inventory availability", ex);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("INVENTORY_SERVICE_UNAVAILABLE",
                    "Unable to validate inventory availability", ex);
        }
    }
}
