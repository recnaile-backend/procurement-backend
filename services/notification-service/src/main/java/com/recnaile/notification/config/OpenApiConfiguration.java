package com.recnaile.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/** OpenAPI metadata and shared error-response documentation for the Notification Service. */
@Configuration
public class OpenApiConfiguration {
    private static final String BEARER_SCHEME = "bearerAuth";
    private static final Map<String, String> COMMON_ERRORS = commonErrors();

    @Bean
    OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RECNAILE Notification Service API")
                        .version("v1")
                        .description("In-app notification inbox and audited event replay APIs. "
                                + "Protected endpoints require a valid JWT issued by the Auth Service. "
                                + "The token must include a non-blank employeeId claim. "
                                + "Error bodies may differ between validation, application, Spring Security, "
                                + "and infrastructure failures; clients should rely on the HTTP status and "
                                + "must not assume every error response has the same JSON body."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /** Adds common possible HTTP error statuses to every documented operation. */
    @Bean
    OpenApiCustomizer commonApiErrorResponses() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        ApiResponses existingResponses = operation.getResponses();
                        final ApiResponses responses;
                        if (existingResponses == null) {
                            responses = new ApiResponses();
                            operation.setResponses(responses);
                        } else {
                            responses = existingResponses;
                        }
                        COMMON_ERRORS.forEach((status, description) ->
                                responses.putIfAbsent(status, new ApiResponse().description(description)));
                    }));
        };
    }

    private static Map<String, String> commonErrors() {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("400", "Bad Request — malformed parameters/path values or rejected operation input.");
        errors.put("401", "Unauthorized — missing, invalid, or expired JWT; invalid issuer/signature; or missing employeeId claim.");
        errors.put("403", "Forbidden — authenticated caller lacks the required authority.");
        errors.put("404", "Not Found — requested notification/event does not exist or is unavailable to the caller.");
        errors.put("405", "Method Not Allowed — HTTP method is not supported for this path.");
        errors.put("409", "Conflict — request conflicts with resource or processing state, where applicable.");
        errors.put("413", "Payload Too Large — request body exceeds configured server limits, where applicable.");
        errors.put("415", "Unsupported Media Type — request content type is unsupported, where applicable.");
        errors.put("422", "Unprocessable Content — input fails semantic validation, where applicable.");
        errors.put("429", "Too Many Requests — configured rate limit exceeded, where applicable.");
        errors.put("500", "Internal Server Error — unexpected application failure.");
        errors.put("502", "Bad Gateway — upstream dependency returned an invalid response, where applicable.");
        errors.put("503", "Service Unavailable — dependency/operation is temporarily unavailable; audited replay may return this when audit publication fails.");
        return Map.copyOf(errors);
    }
}
