package com.recnaile.inward.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PurchaseOrderClientConfig {

    @Bean
    RestClient purchaseOrderRestClientTemplate(
            @Value("${clients.purchase-order.base-url:http://purchase-order-service:8084}")
            String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
