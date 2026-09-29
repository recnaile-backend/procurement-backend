package com.recnaile.outward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recnaile.outward.config.OpenApiConfig;
import com.recnaile.outward.controller.OutwardApprovalController;
import com.recnaile.outward.controller.OutwardIssueController;
import com.recnaile.outward.controller.OutwardRequestController;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class OpenApiContractTest {

    @Test
    void definesJwtSecuritySchemeAndApiMetadata() {
        OpenAPIDefinition definition =
                OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

        SecurityScheme scheme =
                OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        assertNotNull(definition);
        assertEquals("v1", definition.info().version());
        assertNotNull(scheme);
        assertEquals("bearerAuth", scheme.name());
    }

    @Test
    void documentsAllOutwardPostOperations() {
        assertOperation(
                OutwardRequestController.class,
                "create",
                "/api/v1/outward"
        );

        assertOperation(
                OutwardApprovalController.class,
                "submit",
                "/api/v1/outward/{outwardNumber}/submit"
        );

        assertOperation(
                OutwardApprovalController.class,
                "approve",
                "/api/v1/outward/{outwardNumber}/approve"
        );

        assertOperation(
                OutwardApprovalController.class,
                "reject",
                "/api/v1/outward/{outwardNumber}/reject"
        );

        assertOperation(
                OutwardIssueController.class,
                "issue",
                "/api/v1/outward/{outwardNumber}/issue"
        );
    }

    private void assertOperation(
            Class<?> controller,
            String methodName,
            String expectedPath) {

        Method method = java.util.Arrays.stream(controller.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow();

        Operation operation = method.getAnnotation(Operation.class);
        assertNotNull(operation);

        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);

        RequestMapping controllerMapping =
                controller.getAnnotation(RequestMapping.class);

        assertNotNull(controllerMapping);

        String controllerPath = firstPath(controllerMapping.path(),
                controllerMapping.value());

        String methodPath = firstPath(mapping.path(), mapping.value());

        String actualPath = controllerPath + methodPath;

        assertEquals(expectedPath, actualPath);

        assertTrue(
                controller.isAnnotationPresent(SecurityRequirement.class)
        );
    }

    private String firstPath(String[] paths, String[] values) {
        if (paths.length > 0) {
            return paths[0];
        }

        if (values.length > 0) {
            return values[0];
        }

        return "";
    }
}