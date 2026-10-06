package com.recnaile.asset.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.RecordComponent;

import com.recnaile.asset.exception.ApiError;
import org.junit.jupiter.api.Test;

class ApiErrorContractTest {

    @Test
    void apiErrorContainsStableContractFields() {
        RecordComponent[] components = ApiError.class.getRecordComponents();
        assertEquals(8, components.length);
        assertNotNull(ApiError.class.getRecordComponent("timestamp"));
        assertNotNull(ApiError.class.getRecordComponent("status"));
        assertNotNull(ApiError.class.getRecordComponent("error"));
        assertNotNull(ApiError.class.getRecordComponent("code"));
        assertNotNull(ApiError.class.getRecordComponent("message"));
        assertNotNull(ApiError.class.getRecordComponent("path"));
        assertNotNull(ApiError.class.getRecordComponent("traceId"));
        assertNotNull(ApiError.class.getRecordComponent("correlationId"));
    }
}
