package com.recnaile.outward.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI outwardServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RECNAILE Outward Service API")
                        .version("v1")
                        .description("Foundation for outward stock issue requests and issue transactions."));
    }
}
