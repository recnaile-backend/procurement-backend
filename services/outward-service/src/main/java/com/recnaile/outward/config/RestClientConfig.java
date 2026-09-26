package com.recnaile.outward.config;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient restClient(
            @Value("${app.clients.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.clients.read-timeout-ms:3000}") int readTimeoutMs) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        ClientHttpRequestInterceptor contextPropagation = (request, body, execution) -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest current = attributes.getRequest();
                copyHeader(current, request, "Authorization");
                copyHeader(current, request, "X-Correlation-Id");
                copyHeader(current, request, "traceparent");
            }
            return execution.execute(request, body);
        };

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(contextPropagation)
                .build();
    }

    private void copyHeader(HttpServletRequest source,
                            org.springframework.http.HttpRequest target,
                            String name) {
        String value = source.getHeader(name);
        if (value != null && !value.isBlank()) {
            target.getHeaders().set(name, value);
        }
    }
}
