package com.recnaile.returns.integration.outward;

import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.dto.IssuedReturnValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class OutwardServiceRestClientTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsAuthenticatedBearerTokenToOutwardService() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String tokenValue = "test-jwt-token";
        Jwt jwt = new Jwt(tokenValue, Instant.now().minusSeconds(10), Instant.now().plusSeconds(300),
                Map.of("alg", "HS256"), Map.of("sub", "user-1", "employeeId", "EMP001", "roles", List.of("MD")));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        server.expect(requestTo("http://outward-service/api/v1/outward/return-validation?outwardId=OUT-1&itemId=ITEM-1"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer " + tokenValue))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"outwardId\":\"OUT-1\",\"itemId\":\"ITEM-1\",\"issuedQuantity\":5,\"alreadyReturnedQuantity\":2},\"error\":null}",
                        MediaType.APPLICATION_JSON));

        OutwardServiceRestClient client = new OutwardServiceRestClient(builder, "http://outward-service");
        IssuedReturnValidation result = client.validateIssuedItem("OUT-1", "ITEM-1");

        assertThat(result.outwardId()).isEqualTo("OUT-1");
        assertThat(result.itemId()).isEqualTo("ITEM-1");
        assertThat(result.remainingQuantity()).isEqualTo(3);
        server.verify();
    }
}
