package com.recnaile.reporting.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ReportingExceptionHandlerTest {
    private final ReportingExceptionHandler handler = new ReportingExceptionHandler();

    @AfterEach
    void clearMdc() { MDC.clear(); }

    @Test
    void invalidRequestUsesProblemJsonAndCorrelationId() {
        MockHttpServletRequest request = request();
        MDC.put("correlationId", "test-correlation-123");

        var response = handler.badRequest(new IllegalArgumentException("Unsupported sort field: privateField"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("application/problem+json", response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals(400, response.getBody().status());
        assertEquals("test-correlation-123", response.getBody().correlationId());
        assertEquals("/api/v1/reporting/reports/events", response.getBody().instance().toString());
    }

    @Test
    void dataStoreFailureDoesNotLeakInternalExceptionDetails() {
        HttpServletRequest request = request();

        var response = handler.dataUnavailable(new DataAccessResourceFailureException("private database detail"), request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("REPORTING_DATA_UNAVAILABLE", response.getBody().code());
        assertFalse(response.getBody().detail().contains("private database detail"));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/reporting/reports/events");
        return request;
    }
}
