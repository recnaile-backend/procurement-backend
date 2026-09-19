package com.recnaile.purchaseorder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class MasterDataClientConfig {

    @Bean
    RestClient masterDataRestClient(
            @Value("${app.master-data.base-url:http://master-data-service:8082}") String baseUrl,
            @Value("${app.master-data.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${app.master-data.read-timeout-ms:5000}") int readTimeoutMs) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    Authentication authentication =
                            SecurityContextHolder.getContext().getAuthentication();

                    if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                        request.getHeaders().setBearerAuth(
                                jwtAuthenticationToken.getToken().getTokenValue());
                    }

                    String correlationId = org.slf4j.MDC.get("correlationId");
                    if (correlationId != null && !correlationId.isBlank()) {
                        request.getHeaders().set("X-Correlation-Id", correlationId);
                    }

                    return execution.execute(request, body);
                })
                .build();
    }
}
