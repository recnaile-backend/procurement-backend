package com.recnaile.returns.controller;

import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.exception.IdempotencyConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReturnExceptionHandlerTest {

    private final ReturnExceptionHandler handler = new ReturnExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void validationErrorIncludesTraceIdWhenTracingIsAvailable() {
        MDC.put("traceId", "trace-123");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequest(
                new IllegalArgumentException("invalid return"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("INVALID_RETURN_REQUEST", response.getBody().error().code());
        assertEquals("trace-123", response.getBody().error().traceId());
    }

    @Test
    void unexpectedErrorDoesNotExposeInternalMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(
                new RuntimeException("database password=secret"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().error().code());
        assertEquals("An unexpected error occurred", response.getBody().error().message());
    }

    @Test
    void idempotencyConflictUsesStableErrorCode() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleIdempotencyConflict(
                new IdempotencyConflictException("same key used with different request"));

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("IDEMPOTENCY_CONFLICT", response.getBody().error().code());
    }
}
