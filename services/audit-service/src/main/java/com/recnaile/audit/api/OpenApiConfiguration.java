package com.recnaile.audit.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {
    private static final String ERROR_SCHEMA = "AuditApiErrorResponse";

    @Bean
    OpenAPI auditServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RECNAILE Audit Service API")
                        .version("1.0.0")
                        .description("Audit event history, retention preview, and restricted event replay. Protected endpoints require a valid Auth Service JWT."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas(ERROR_SCHEMA, new ObjectSchema()
                                .addProperty("success", new BooleanSchema()._default(false))
                                .addProperty("data", new ObjectSchema().nullable(true))
                                .addProperty("error", new ObjectSchema()
                                        .addProperty("code", new StringSchema().example("FORBIDDEN"))
                                        .addProperty("message", new StringSchema().example("You are not authorized to access this audit data"))
                                        .addProperty("traceId", new StringSchema().nullable(true)))))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer auditCommonErrorResponses() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                    operation.setResponses(responses);
                }
                addIfMissing(responses, "400", "Invalid request, UUID, or pagination/input value");
                addIfMissing(responses, "401", "Missing, invalid, expired, or wrong-issuer bearer token");
                addIfMissing(responses, "403", "Authenticated caller does not have permission for this data or operation");
                addIfMissing(responses, "404", "Requested audit event does not exist (where applicable)");
                addIfMissing(responses, "500", "Unexpected server error; use traceId when available for troubleshooting");
            }));
        };
    }

    private static void addIfMissing(ApiResponses responses, String status, String description) {
        if (responses.containsKey(status)) return;
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA)))));
    }
}
