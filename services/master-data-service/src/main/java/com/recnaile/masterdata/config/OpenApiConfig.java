package com.recnaile.masterdata.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.recnaile.masterdata.dto.ErrorResponse;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI masterDataOpenAPI() {

        Components components = new Components();

        /*
         * JWT Bearer authentication
         */
        components.addSecuritySchemes(
                "bearerAuth",
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                                "Enter JWT access token. Example: Bearer eyJhbGciOi..."
                        )
        );

        /*
         * Register ErrorResponse using Swagger's ModelConverters.
         *
         * This is more reliable than manually constructing the schema
         * because Springdoc/Swagger can resolve the DTO exactly as it
         * appears in the application.
         */
        ResolvedSchema resolvedSchema =
                ModelConverters.getInstance()
                        .resolveAsResolvedSchema(
                                new AnnotatedType(ErrorResponse.class)
                                        .resolveAsRef(true)
                        );

        if (resolvedSchema.referencedSchemas != null) {

            resolvedSchema.referencedSchemas.forEach(
                    components::addSchemas
            );
        }

        /*
         * Fallback registration.
         *
         * If ErrorResponse was not added by ModelConverters for any reason,
         * explicitly register a basic schema so every $ref can resolve.
         */
        if (components.getSchemas() == null
                || !components.getSchemas().containsKey("ErrorResponse")) {

            Schema<Object> errorResponseSchema =
                    new Schema<>();

            errorResponseSchema
                    .type("object")
                    .addProperty(
                            "timestamp",
                            new Schema<>()
                                    .type("string")
                                    .format("date-time")
                    )
                    .addProperty(
                            "status",
                            new Schema<>()
                                    .type("integer")
                                    .format("int32")
                                    .example(403)
                    )
                    .addProperty(
                            "error",
                            new Schema<>()
                                    .type("string")
                                    .example("Forbidden")
                    )
                    .addProperty(
                            "message",
                            new Schema<>()
                                    .type("string")
                                    .example(
                                            "The authenticated user does not have the required role."
                                    )
                    )
                    .addProperty(
                            "path",
                            new Schema<>()
                                    .type("string")
                                    .example(
                                            "/api/v1/master-data/departments"
                                    )
                    );

            components.addSchemas(
                    "ErrorResponse",
                    errorResponseSchema
            );
        }

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Master Data Service API")
                                .version("v1")
                                .description(
                                        "Master Data APIs for departments, projects, vendors and items."
                                )
                )
                .components(components);
    }

    @Bean
    public OpenApiCustomizer globalErrorResponses() {

        return openAPI -> {

            if (openAPI.getPaths() == null) {
                return;
            }

            /*
             * Make absolutely sure ErrorResponse exists before adding
             * references to it.
             */
            if (openAPI.getComponents() == null) {
                openAPI.setComponents(new Components());
            }

            if (openAPI.getComponents().getSchemas() == null) {
                openAPI.getComponents().setSchemas(
                        new java.util.LinkedHashMap<>()
                );
            }

            if (!openAPI.getComponents()
                    .getSchemas()
                    .containsKey("ErrorResponse")) {

                Schema<Object> errorResponseSchema =
                        new Schema<>();

                errorResponseSchema
                        .type("object")
                        .addProperty(
                                "timestamp",
                                new Schema<>()
                                        .type("string")
                                        .format("date-time")
                        )
                        .addProperty(
                                "status",
                                new Schema<>()
                                        .type("integer")
                                        .format("int32")
                        )
                        .addProperty(
                                "error",
                                new Schema<>()
                                        .type("string")
                        )
                        .addProperty(
                                "message",
                                new Schema<>()
                                        .type("string")
                        )
                        .addProperty(
                                "path",
                                new Schema<>()
                                        .type("string")
                        );

                openAPI.getComponents()
                        .addSchemas(
                                "ErrorResponse",
                                errorResponseSchema
                        );
            }

            /*
             * Add standard 401 and 403 responses to every endpoint.
             */
            openAPI.getPaths()
                    .values()
                    .forEach(pathItem ->

                            pathItem.readOperations()
                                    .forEach(operation -> {

                                        ApiResponses responses =
                                                operation.getResponses();

                                        if (responses == null) {
                                            responses = new ApiResponses();
                                            operation.setResponses(responses);
                                        }

                                        addResponse(
                                                responses,
                                                "401",
                                                "Unauthorized",
                                                "Authentication is required or the JWT token is invalid."
                                        );

                                        addResponse(
                                                responses,
                                                "403",
                                                "Forbidden",
                                                "The authenticated user does not have the required role."
                                        );
                                    })
                    );
        };
    }

    private void addResponse(
            ApiResponses responses,
            String status,
            String description,
            String message) {

        /*
         * Don't overwrite endpoint-specific responses.
         */
        if (responses.containsKey(status)) {
            return;
        }

        ApiResponse apiResponse =
                new ApiResponse()
                        .description(description)
                        .content(
                                new Content()
                                        .addMediaType(
                                                "application/json",
                                                new MediaType()
                                                        .schema(
                                                                new Schema<>()
                                                                        .$ref(
                                                                                "#/components/schemas/ErrorResponse"
                                                                        )
                                                        )
                                        )
                        );

        responses.addApiResponse(
                status,
                apiResponse
        );
    }
}