package com.recnaile.indent.config;

import com.recnaile.indent.client.masterdata.MasterDataClient;
import com.recnaile.indent.client.masterdata.MasterDataProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MasterDataProperties.class)
public class MasterDataClientConfig {

    @Bean
    RestClient masterDataRestClient(MasterDataProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(
                        MasterDataClient.requestFactory(
                                properties.getConnectTimeout(),
                                properties.getReadTimeout()))
                .requestInterceptor((request, body, execution) -> {
                    Authentication authentication =
                            SecurityContextHolder.getContext().getAuthentication();

                    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                        request.getHeaders().setBearerAuth(
                                jwtAuthentication.getToken().getTokenValue());
                    }

                    return execution.execute(request, body);
                })
                .build();
    }

    @Bean
    MasterDataClient masterDataClient(RestClient masterDataRestClient) {
        return new MasterDataClient(masterDataRestClient);
    }
}
