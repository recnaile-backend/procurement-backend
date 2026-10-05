package com.recnaile.returns.integration.outward;

import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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
    public IssuedReturnValidation validateIssuedLine(String outwardId, String outwardLineId) {
        try {
            ApiResponse<IssuedReturnValidation> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/outwards/{outwardId}/lines/{outwardLineId}/return-validation")
                            .build(outwardId, outwardLineId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                        throw new ReturnRequestException(
                                "Unable to validate issued outward line: HTTP " + httpResponse.getStatusCode().value());
                    })
                    .body(new org.springframework.core.ParameterizedTypeReference<ApiResponse<IssuedReturnValidation>>() {});

            if (response == null || !response.success() || response.data() == null) {
                throw new ReturnRequestException("Issued outward line was not found or could not be validated");
            }
            return response.data();
        } catch (ReturnRequestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ReturnRequestException("Unable to validate issued outward line");
        }
    }
}
