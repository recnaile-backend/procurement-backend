package com.recnaile.returns.integration.outward;

import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.exception.DownstreamServiceException;
import com.recnaile.returns.observability.CorrelationIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class OutwardServiceRestClient implements IssuedReturnValidationClient {

    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public OutwardServiceRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.integrations.outward-service.base-url:http://outward-service:8087}") String baseUrl,
            @Value("${app.integrations.outward-service.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.integrations.outward-service.read-timeout-ms:5000}") long readTimeoutMs) {
        this.restClient = restClientBuilder
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .baseUrl(baseUrl)
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory requestFactory(
            long connectTimeoutMs,
            long readTimeoutMs) {
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(Math.max(1, connectTimeoutMs)))
                        .build());
        factory.setReadTimeout(Duration.ofMillis(Math.max(1, readTimeoutMs)));
        return factory;
    }

    /**
     * Test-friendly constructor retaining the original RestClient.Builder contract.
     */
    public OutwardServiceRestClient(RestClient.Builder restClientBuilder, String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public IssuedReturnValidation validateIssuedItem(String outwardId, String itemId) {
        if (outwardId == null || outwardId.isBlank()) {
            throw new IllegalArgumentException("outwardId must not be blank");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }

        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/outward/return-validation")
                            .queryParam("outwardId", outwardId.trim())
                            .queryParam("itemId", itemId.trim())
                            .build());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                request.header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + jwtAuthentication.getToken().getTokenValue());
            }

            String correlationId = org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null && !correlationId.isBlank()) {
                request.header(CorrelationIdFilter.HEADER_NAME, correlationId);
            }

            ApiResponse<IssuedReturnValidation> response = request
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (errorRequest, httpResponse) -> {
                        throw new DownstreamServiceException("Outward Service returned HTTP " + httpResponse.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (errorRequest, httpResponse) -> {
                        throw new ReturnRequestException("Unable to validate issued outward item: HTTP " + httpResponse.getStatusCode().value());
                    })
                    .body(new org.springframework.core.ParameterizedTypeReference<
                            ApiResponse<IssuedReturnValidation>>() {});

            if (response == null || !response.success() || response.data() == null) {
                throw new ReturnRequestException(
                        "Issued outward item was not found or could not be validated");
            }
            return response.data();
        } catch (ReturnRequestException | DownstreamServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DownstreamServiceException("Unable to reach Outward Service while validating the issued item", exception);
        }
    }
}
