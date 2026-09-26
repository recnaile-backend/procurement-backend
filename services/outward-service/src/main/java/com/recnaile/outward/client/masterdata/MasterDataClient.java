package com.recnaile.outward.client.masterdata;

import com.recnaile.outward.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;

@Component
public class MasterDataClient {

    private final RestClient restClient;
    private final String baseUrl;

    public MasterDataClient(
            RestClient restClient,
            @Value("${app.clients.master-data-base-url:http://master-data-service:8082}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public ItemResponse getItem(String itemId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/v1/master-data/items/{itemId}", itemId)
                    .retrieve()
                    .body(ItemResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw unavailable("Unable to validate item master data", ex);
        } catch (RestClientException ex) {
            throw unavailable("Unable to validate item master data", ex);
        }
    }

    public List<DepartmentResponse> getDepartments() {
        try {
            DepartmentResponse[] response = restClient.get()
                    .uri(baseUrl + "/api/v1/master-data/departments")
                    .retrieve()
                    .body(DepartmentResponse[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            throw unavailable("Unable to validate department master data", ex);
        }
    }

    public ProjectResponse getProject(String projectId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/v1/master-data/projects/{projectId}", projectId)
                    .retrieve()
                    .body(ProjectResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw unavailable("Unable to validate project master data", ex);
        } catch (RestClientException ex) {
            throw unavailable("Unable to validate project master data", ex);
        }
    }

    private DownstreamServiceException unavailable(String message, Throwable cause) {
        return new DownstreamServiceException(
                "MASTER_DATA_SERVICE_UNAVAILABLE", message, cause);
    }
}
