package com.recnaile.purchaseorder.client.masterdata;

import com.recnaile.purchaseorder.exception.MasterDataIntegrationException;
import com.recnaile.purchaseorder.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MasterDataVendorRestClient implements MasterDataVendorClient {

    private final RestClient restClient;

    public MasterDataVendorRestClient(RestClient masterDataRestClient) {
        this.restClient = masterDataRestClient;
    }

    @Override
    public VendorResponse getVendor(String vendorId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/master-data/vendors/{vendorId}")
                            .build(vendorId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ResourceNotFoundException(
                                    "VENDOR_NOT_FOUND",
                                    "Vendor not found in master-data-service: " + vendorId);
                        }
                        throw new MasterDataIntegrationException(
                                "MASTER_DATA_CLIENT_ERROR",
                                "Master-data-service rejected the vendor lookup with HTTP "
                                        + response.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) ->
                            new MasterDataIntegrationException(
                                    "MASTER_DATA_UNAVAILABLE",
                                    "Master-data-service is unavailable while resolving vendor: " + vendorId))
                    .body(VendorResponse.class);
        } catch (ResourceNotFoundException | MasterDataIntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataIntegrationException(
                    "MASTER_DATA_UNAVAILABLE",
                    "Unable to reach master-data-service while resolving vendor: " + vendorId,
                    ex);
        }
    }
}
