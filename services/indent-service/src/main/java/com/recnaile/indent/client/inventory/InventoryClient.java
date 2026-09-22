package com.recnaile.indent.client.inventory;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient restClient) {
        this.restClient = restClient;
    }


    public static SimpleClientHttpRequestFactory requestFactory(
            Duration connectTimeout,
            Duration readTimeout) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return factory;
    }


    /**
     * Looks up current inventory quantity by item name.
     *
     * Inventory Service contract expected by this client:
     *
     * GET /api/v1/inventory/items/by-name/{itemName}
     *
     * Response:
     * {
     *   "itemId": "ITEM-001",
     *   "itemName": "Laptop",
     *   "availableQuantity": 12
     * }
     *
     * A 404 means the item is not present in inventory and is therefore
     * interpreted by Auto Verify as NOT_AVAILABLE.
     */
    public InventoryItemResponse getInventoryByItemName(
            String itemName) {

        try {

            InventoryItemResponse response =
                    restClient.get()
                            .uri(
                                    "/api/v1/inventory/items/by-name/{itemName}",
                                    itemName)
                            .retrieve()

                            .onStatus(
                                    status -> status.value() == 404,
                                    (request, response1) -> {
                                        throw new InventoryException(
                                                InventoryException.Reason.NOT_FOUND,
                                                itemName,
                                                "Item was not found in inventory: "
                                                        + itemName);
                                    })

                            .onStatus(
                                    HttpStatusCode::is4xxClientError,
                                    (request, response1) -> {
                                        throw new InventoryException(
                                                InventoryException.Reason.UNAVAILABLE,
                                                itemName,
                                                "Inventory Service rejected item lookup");
                                    })

                            .onStatus(
                                    HttpStatusCode::is5xxServerError,
                                    (request, response1) -> {
                                        throw new InventoryException(
                                                InventoryException.Reason.UNAVAILABLE,
                                                itemName,
                                                "Inventory Service is unavailable");
                                    })

                            .body(InventoryItemResponse.class);

            if (response == null) {
                throw new InventoryException(
                        InventoryException.Reason.INVALID_RESPONSE,
                        itemName,
                        "Inventory Service returned an empty response");
            }

            if (response.availableQuantity() == null) {
                throw new InventoryException(
                        InventoryException.Reason.INVALID_RESPONSE,
                        itemName,
                        "Inventory Service returned no available quantity");
            }

            return response;

        } catch (InventoryException ex) {
            throw ex;

        } catch (RestClientException ex) {
            throw new InventoryException(
                    InventoryException.Reason.UNAVAILABLE,
                    itemName,
                    "Unable to reach Inventory Service",
                    ex);
        }
    }
}
