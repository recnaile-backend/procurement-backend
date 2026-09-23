package com.recnaile.purchaseorder.client.masterdata;

import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.exception.MasterDataIntegrationException;
import com.recnaile.purchaseorder.exception.ResourceNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class MasterDataReferenceRestClient implements MasterDataReferenceClient {

    private final RestClient restClient;

    public MasterDataReferenceRestClient(RestClient masterDataRestClient) {
        this.restClient = masterDataRestClient;
    }

    @Override
    public DepartmentResponse requireActiveDepartmentByName(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            throw new BusinessRuleException("DEPARTMENT_NAME_REQUIRED", "Department name is required");
        }

        List<DepartmentResponse> departments = getAllDepartments();
        String normalizedName = departmentName.trim();

        List<DepartmentResponse> matches = departments.stream()
                .filter(d -> d != null && d.name() != null
                        && normalizedName.equalsIgnoreCase(d.name().trim()))
                .filter(d -> "ACTIVE".equalsIgnoreCase(d.status()))
                .toList();

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException(
                    "DEPARTMENT_NOT_FOUND",
                    "Active department not found in master-data-service: " + normalizedName);
        }
        if (matches.size() > 1) {
            throw new BusinessRuleException(
                    "DEPARTMENT_NAME_AMBIGUOUS",
                    "Multiple active departments found with the name: " + normalizedName);
        }
        return matches.getFirst();
    }

    @Override
    public ItemResponse requireActiveItem(String itemId, String itemName) {
        if (itemId == null || itemId.isBlank()) {
            throw new BusinessRuleException("ITEM_ID_REQUIRED", "Item ID is required");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new BusinessRuleException("ITEM_NAME_REQUIRED", "Item name is required");
        }

        ItemResponse item = getItem(itemId.trim());
        if (item == null) {
            throw new MasterDataIntegrationException(
                    "MASTER_DATA_INVALID_RESPONSE",
                    "Master-data-service returned an empty item response for: " + itemId);
        }
        if (!"ACTIVE".equalsIgnoreCase(item.status())) {
            throw new BusinessRuleException(
                    "ITEM_INACTIVE",
                    "Item is inactive and cannot be used for a purchase order: " + itemId);
        }
        if (item.name() == null || !itemName.trim().equalsIgnoreCase(item.name().trim())) {
            throw new BusinessRuleException(
                    "ITEM_MASTER_MISMATCH",
                    "Item ID and item name do not match master-data-service: " + itemId);
        }
        return item;
    }

    private List<DepartmentResponse> getAllDepartments() {
        try {
            List<DepartmentResponse> departments = restClient.get()
                    .uri("/master-data/departments")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) ->
                            new MasterDataIntegrationException(
                                    "MASTER_DATA_CLIENT_ERROR",
                                    "Master-data-service rejected the department lookup with HTTP "
                                            + response.getStatusCode().value()))
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) ->
                            new MasterDataIntegrationException(
                                    "MASTER_DATA_UNAVAILABLE",
                                    "Master-data-service is unavailable while resolving departments"))
                    .body(new ParameterizedTypeReference<List<DepartmentResponse>>() {});
            if (departments == null) {
                throw new MasterDataIntegrationException(
                        "MASTER_DATA_INVALID_RESPONSE",
                        "Master-data-service returned no department list");
            }
            return departments;
        } catch (MasterDataIntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataIntegrationException(
                    "MASTER_DATA_UNAVAILABLE",
                    "Unable to reach master-data-service while resolving departments", ex);
        }
    }

    private ItemResponse getItem(String itemId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/master-data/items/{itemId}")
                            .build(itemId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ResourceNotFoundException(
                                    "ITEM_NOT_FOUND",
                                    "Item not found in master-data-service: " + itemId);
                        }
                        throw new MasterDataIntegrationException(
                                "MASTER_DATA_CLIENT_ERROR",
                                "Master-data-service rejected the item lookup with HTTP "
                                        + response.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) ->
                            new MasterDataIntegrationException(
                                    "MASTER_DATA_UNAVAILABLE",
                                    "Master-data-service is unavailable while resolving item: " + itemId))
                    .body(ItemResponse.class);
        } catch (ResourceNotFoundException | MasterDataIntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataIntegrationException(
                    "MASTER_DATA_UNAVAILABLE",
                    "Unable to reach master-data-service while resolving item: " + itemId, ex);
        }
    }
}
