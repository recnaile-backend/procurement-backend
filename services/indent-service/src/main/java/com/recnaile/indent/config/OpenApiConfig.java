package com.recnaile.indent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI indentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RECNAILE Indent Service API")
                        .version("v1")
                        .description("""
                                APIs for the procurement indent lifecycle.

                                Workflow:
                                DRAFT -> SUBMITTED -> UNDER_VERIFICATION -> VERIFIED
                                -> PENDING_APPROVAL -> APPROVED -> READY_FOR_PO

                                Rejection can occur during verification or approval.
                                All protected endpoints require a valid JWT.
                                """)
                        .contact(new Contact().name("RECNAILE Procurement Platform")))
                .servers(java.util.List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Local Indent Service")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
