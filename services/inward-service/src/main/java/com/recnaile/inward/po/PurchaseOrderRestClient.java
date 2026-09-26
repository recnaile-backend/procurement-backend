package com.recnaile.inward.po;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.client.RestClientException;
import com.recnaile.inward.exception.PurchaseOrderNotFoundException;
import org.springframework.core.ParameterizedTypeReference;

@Component
public class PurchaseOrderRestClient implements PurchaseOrderClient {

    private final RestClient restClient;

    public PurchaseOrderRestClient(
            @Qualifier("purchaseOrderRestClientTemplate")
            RestClient purchaseOrderRestClient
    ) {
        this.restClient = purchaseOrderRestClient;
    }

    @Override
    public PurchaseOrderSummary getPurchaseOrder(String poId) {
        try {
            PurchaseOrderApiResponse<PurchaseOrderSummary> envelope = restClient.get()
                    .uri("/api/v1/purchase-orders/{poId}", poId)
                    .headers(headers -> addAuthorizationHeader(headers))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        if (clientResponse.getStatusCode().value() == 404) {
                            throw new PurchaseOrderNotFoundException(poId);
                        }

                        if (clientResponse.getStatusCode().value() == 403) {
                            throw new PurchaseOrderForbiddenException(poId);
                        }

                        throw new PurchaseOrderClientException(
                                "Purchase Order Service returned HTTP "
                                        + clientResponse.getStatusCode().value()
                                        + " for PO " + poId
                        );
                    })
                    .body(new ParameterizedTypeReference<PurchaseOrderApiResponse<PurchaseOrderSummary>>() {});

            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new PurchaseOrderClientException(
                        "Purchase Order Service returned no PO data for PO " + poId
                );
            }

            return envelope.data();
        } catch (PurchaseOrderNotFoundException ex) {
            throw ex;
        } catch (PurchaseOrderForbiddenException ex) {
            throw ex;
        } catch (PurchaseOrderClientException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PurchaseOrderClientException(
                    "Unable to retrieve PO " + poId + " from Purchase Order Service",
                    ex
            );
        }
    }

    private void addAuthorizationHeader(HttpHeaders headers) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return;
        }

        String authorization =
                attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null && !authorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }
}
