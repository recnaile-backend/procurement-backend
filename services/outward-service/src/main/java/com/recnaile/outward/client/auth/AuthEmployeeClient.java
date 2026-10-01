package com.recnaile.outward.client.auth;

import com.recnaile.outward.exception.DownstreamServiceException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthEmployeeClient {

    private final RestClient restClient;
    private final String baseUrl;

    public AuthEmployeeClient(
            RestClient restClient,
            @Value("${app.clients.auth-base-url:http://auth-service:8081}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    /**
     * Auth Service exposes GET /api/v1/auth/users for MD callers. The endpoint
     * returns RegisterResponse objects (not the internal User document).
     * RestClientConfig propagates the caller's Authorization header.
     */
    @Observed(name = "outward.auth.employees", contextualName = "get-auth-employees")
    public List<EmployeeResponse> getEmployees() {
        try {
            EmployeeResponse[] response = restClient.get()
                    .uri(baseUrl + "/api/v1/auth/users")
                    .retrieve()
                    .body(EmployeeResponse[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return List.of();
            }
            if (ex.getStatusCode().value() == 403) {
                throw new DownstreamServiceException(
                        "AUTH_EMPLOYEE_LOOKUP_FORBIDDEN",
                        "Auth Service employee lookup requires an authorized MD caller", ex);
            }
            throw unavailable("Unable to validate recipient employee", ex);
        } catch (RestClientException ex) {
            throw unavailable("Unable to validate recipient employee", ex);
        }
    }

    private DownstreamServiceException unavailable(String message, Throwable cause) {
        return new DownstreamServiceException(
                "AUTH_SERVICE_UNAVAILABLE", message, cause);
    }
}
