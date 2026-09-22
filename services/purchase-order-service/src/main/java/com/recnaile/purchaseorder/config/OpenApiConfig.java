package com.recnaile.purchaseorder.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI purchaseOrderOpenAPI() {
        Schema<?> errorSchema = new Schema<>()
                .type("object")
                .addProperty("success", new Schema<>().type("boolean").example(false))
                .addProperty("data", new Schema<>().nullable(true).example(null))
                .addProperty("error", new Schema<>()
                        .type("object")
                        .addProperty("code", new Schema<>().type("string").example("PO_NOT_FOUND"))
                        .addProperty("message", new Schema<>().type("string").example("Purchase Order not found."))
                        .addProperty("traceId", new Schema<>().type("string").example("4bf92f3577b34da6a3ce929d0e0e4736")));

        Schema<?> successSchema = new Schema<>()
                .type("object")
                .addProperty("success", new Schema<>().type("boolean").example(true))
                .addProperty("data", new Schema<>().type("object"))
                .addProperty("error", new Schema<>().nullable(true).example(null));

        return new OpenAPI()
                .info(new Info()
                        .title("Purchase Order Service API")
                        .version("v1")
                        .description("""
                                REST API for Purchase Order lifecycle management.

                                Authentication:
                                - JWT Bearer token is required.
                                - Roles supported by this service: Employee, Procurement, TL, MD.
                                - Employee has no Purchase Order API access.
                                - TL is read-only.
                                - Procurement can view/create/issue/cancel/complete.
                                - MD has full Purchase Order access.

                                Swagger UI: /swagger-ui.html
                                OpenAPI JSON: /v3/api-docs

                                Command APIs support an optional Idempotency-Key. When supplied, retries with the same key and request are replay-safe; reusing a key for a different request is rejected. The header may be omitted when idempotency protection is not needed.
                                Business state transitions are validated server-side and optimistic concurrency is enforced.

                                Error responses use the platform error envelope. The error object contains a stable
                                machine-readable code, a human-readable message, and a traceId for troubleshooting.
                                """)
                        .contact(new Contact().name("RECNAILE Procurement Platform")))
                .components(new Components()
                        .addSchemas("ApiErrorResponse", errorSchema)
                        .addSchemas("ApiSuccessResponse", successSchema)
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList("bearerAuth"));
    }
}
