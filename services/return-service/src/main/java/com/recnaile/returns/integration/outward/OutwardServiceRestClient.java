package com.recnaile.returns.integration.outward;

import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OutwardServiceRestClient implements IssuedReturnValidationClient {

    private final RestClient restClient;

    public OutwardServiceRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.integrations.outward-service.base-url:http://outward-service:8087}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public IssuedReturnValidation validateIssuedItem(String outwardId, String itemId) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/outward/return-validation")
                            .queryParam("outwardId", outwardId)
                            .queryParam("itemId", itemId)
                            .build());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtAuthentication.getToken().getTokenValue());
            }

            ApiResponse<IssuedReturnValidation> response = request
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (errorRequest, httpResponse) -> {
                        throw new ReturnRequestException(
                                "Unable to validate issued outward item: HTTP " + httpResponse.getStatusCode().value());
                    })
                    .body(new org.springframework.core.ParameterizedTypeReference<ApiResponse<IssuedReturnValidation>>() {});

            if (response == null || !response.success() || response.data() == null) {
                throw new ReturnRequestException("Issued outward item was not found or could not be validated");
            }
            return response.data();
        } catch (ReturnRequestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ReturnRequestException("Unable to validate issued outward item");
        }
    }
}
