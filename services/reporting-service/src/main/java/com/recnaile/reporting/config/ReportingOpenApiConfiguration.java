package com.recnaile.reporting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportingOpenApiConfiguration {
    @Bean
    OpenAPI reportingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Procurement Reporting Service API")
                        .version("1.0.0")
                        .description("Read-only reporting dashboards, period summaries, event facts, snapshots, and restricted recovery operations. Errors use a consistent RFC 7807-style ApiProblem body. Reporting data is eventually consistent.")
                        .contact(new Contact().name("Procurement Platform Team")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSchemas("ApiProblem", new Schema<>().type("object")
                                .addProperty("type", new Schema<String>().type("string").format("uri"))
                                .addProperty("title", new Schema<String>().type("string"))
                                .addProperty("status", new Schema<Integer>().type("integer").format("int32"))
                                .addProperty("detail", new Schema<String>().type("string"))
                                .addProperty("instance", new Schema<String>().type("string").format("uri"))
                                .addProperty("code", new Schema<String>().type("string"))
                                .addProperty("correlationId", new Schema<String>().type("string"))))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer reportingErrorResponses() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                if (responses == null) { responses = new ApiResponses(); operation.setResponses(responses); }
                addError(responses, "400", "Invalid parameters, filters, date ranges, or sort options.");
                addError(responses, "401", "Missing, expired, or invalid bearer token.");
                addError(responses, "403", "The authenticated user does not have the required role or reporting scope.");
                addError(responses, "404", "The requested route or resource was not found.");
                addError(responses, "405", "The HTTP method is not supported for this endpoint.");
                addError(responses, "415", "The request content type is not supported.");
                addError(responses, "500", "Unexpected server-side failure. Use the correlation ID when contacting support.");
                addError(responses, "503", "The reporting data store is temporarily unavailable.");
            }));
        };
    }

    private void addError(ApiResponses responses, String code, String description) {
        if (responses.containsKey(code)) return;
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/ApiProblem");
        Content content = new Content().addMediaType("application/problem+json", new MediaType().schema(schema));
        responses.addApiResponse(code, new ApiResponse().description(description).content(content));
    }
}
