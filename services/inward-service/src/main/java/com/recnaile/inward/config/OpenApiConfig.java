package com.recnaile.inward.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inwardServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recnaile Inward / GRN Service API")
                        .version("1.0.0")
                        .description("REST APIs for Goods Receipt Notes. The list API optionally accepts poId; when omitted it returns all GRNs. All business responses use the standard success/data/error envelope. Error responses document stable error codes and HTTP semantics."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth"));
    }
}
