package com.recnaile.asset.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import com.recnaile.asset.exception.ApiError;
import org.junit.jupiter.api.Test;

class ApiErrorContractTest {

    @Test
    void apiErrorContainsStableContractFields() {
        var components = ApiError.class.getRecordComponents();
        assertNotNull(components);
        assertEquals(8, components.length);
        var names = Arrays.stream(components).map(java.lang.reflect.RecordComponent::getName).toList();
        assertEquals(
                java.util.List.of("timestamp", "status", "error", "code", "message", "path", "traceId", "correlationId"),
                names);
    }
}
