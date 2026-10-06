package com.recnaile.returns.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigurationTest {

    @Test
    void exposesBearerSecuritySchemeAndServiceMetadata() {
        OpenAPI api = new OpenApiConfiguration().returnServiceOpenAPI();

        assertNotNull(api.getInfo());
        assertEquals("RECNAILE Return Service API", api.getInfo().getTitle());
        assertNotNull(api.getComponents());
        assertNotNull(api.getComponents().getSecuritySchemes().get("bearerAuth"));
        assertEquals("bearer", api.getComponents().getSecuritySchemes().get("bearerAuth").getScheme());
        assertEquals(1, api.getSecurity().size());
    }
}
