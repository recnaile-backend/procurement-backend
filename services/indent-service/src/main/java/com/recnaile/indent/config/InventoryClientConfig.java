package com.recnaile.indent.config;

import com.recnaile.indent.client.inventory.InventoryClient;
import com.recnaile.indent.client.inventory.InventoryProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(InventoryProperties.class)
public class InventoryClientConfig {

    @Bean
    RestClient inventoryRestClient(
            InventoryProperties properties) {

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(
                        InventoryClient.requestFactory(
                                properties.getConnectTimeout(),
                                properties.getReadTimeout()))
                .requestInterceptor((request, body, execution) -> {

                    Authentication authentication =
                            SecurityContextHolder
                                    .getContext()
                                    .getAuthentication();

                    /*
                     * Propagate the authenticated user's JWT to the
                     * Inventory Service.
                     */
                    if (authentication
                            instanceof JwtAuthenticationToken jwtAuthentication) {

                        request.getHeaders().setBearerAuth(
                                jwtAuthentication
                                        .getToken()
                                        .getTokenValue());
                    }

                    return execution.execute(
                            request,
                            body);
                })
                .build();
    }

    @Bean
    InventoryClient inventoryClient(
            RestClient inventoryRestClient) {

        return new InventoryClient(
                inventoryRestClient);
    }
}
