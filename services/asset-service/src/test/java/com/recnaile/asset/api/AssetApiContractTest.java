package com.recnaile.asset.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class AssetApiContractTest {

    @Test
    void controllerExposesExpectedBasePath() {
        RequestMapping mapping = AssetController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/api/v1/assets", mapping.value()[0]);
    }

    @Test
    void controllerExposesAllDocumentedOperations() {
        Map<String, String> routes = Arrays.stream(AssetController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotations())
                        .filter(annotation -> annotation instanceof GetMapping || annotation instanceof PostMapping)
                        .map(annotation -> {
                            String path;
                            String verb;
                            if (annotation instanceof GetMapping get) {
                                path = get.value().length == 0 ? "" : get.value()[0];
                                verb = "GET";
                            } else {
                                PostMapping post = (PostMapping) annotation;
                                path = post.value().length == 0 ? "" : post.value()[0];
                                verb = "POST";
                            }
                            return Map.entry(verb + " " + path, method.getName());
                        }))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertTrue(routes.containsKey("POST "), "Create endpoint missing");
        assertTrue(routes.containsKey("GET "), "List endpoint missing");
        assertTrue(routes.containsKey("GET /by-issue"));
        assertTrue(routes.containsKey("POST /assign"));
        assertTrue(routes.containsKey("POST /return"));
        assertTrue(routes.containsKey("POST /repair/complete"));
        assertTrue(routes.containsKey("POST /retire"));
        assertTrue(routes.containsKey("GET /history"));
        assertTrue(routes.containsKey("GET /history/employee/{employeeId}"));
        assertTrue(routes.containsKey("GET /history/department/{departmentName}"));
        assertEquals(10, routes.size());
    }

    @Test
    void everyEndpointHasOpenApiOperationDocumentation() {
        for (Method method : AssetController.class.getDeclaredMethods()) {
            boolean endpoint = method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class);
            if (endpoint) {
                assertNotNull(method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class),
                        "Missing @Operation on " + method.getName());
            }
        }
    }
}
