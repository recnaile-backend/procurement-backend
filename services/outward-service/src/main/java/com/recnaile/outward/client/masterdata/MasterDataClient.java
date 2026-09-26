package com.recnaile.outward.client.masterdata;

import com.recnaile.outward.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MasterDataClient {

    private final RestClient restClient;
    private final String baseUrl;

    public MasterDataClient(RestClient restClient,
                            @Value("${app.clients.master-data-base-url:http://master-data-service:8082}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public ItemResponse item(String itemId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/v1/master-data/items/{itemId}", itemId)
                    .retrieve()
                    .body(ItemResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return null;
            throw new DownstreamServiceException("MASTER_DATA_SERVICE_UNAVAILABLE",
                    "Unable to validate item master data", ex);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("MASTER_DATA_SERVICE_UNAVAILABLE",
                    "Unable to validate item master data", ex);
        }
    }

    public ReferenceResponse department(String departmentId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/v1/master-data/departments/{id}", departmentId)
                    .retrieve()
                    .body(ReferenceResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return null;
            throw new DownstreamServiceException("MASTER_DATA_SERVICE_UNAVAILABLE",
                    "Unable to validate department master data", ex);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("MASTER_DATA_SERVICE_UNAVAILABLE",
                    "Unable to validate department master data", ex);
        }
    }

    public ReferenceResponse project(String projectId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/v1/master-data/projects/{id}", projectId)
                    .retrieve()
                    .body(ReferenceResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return null;
            throw new DownstreamServiceException("MASTER_DATA_SERVICE_UNAVAILABLE",
                    "Unable to validate project master data", ex);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("MASTER_DATA_SERVICE_UNAVAILABLE",
                    "Unable to validate project master data", ex);
        }
    }
}
