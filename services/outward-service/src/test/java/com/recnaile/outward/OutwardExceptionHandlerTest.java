package com.recnaile.outward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.recnaile.outward.dto.ApiError;
import com.recnaile.outward.exception.DownstreamServiceException;
import com.recnaile.outward.exception.MasterDataValidationException;
import com.recnaile.outward.exception.OutwardException;
import com.recnaile.outward.exception.OutwardExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class OutwardExceptionHandlerTest {

    private final OutwardExceptionHandler handler = new OutwardExceptionHandler();

    @Test
    void mapsNotFoundTo404() {
        ResponseEntity<ApiError> response = handler.handleOutward(
                new OutwardException("OUTWARD_NOT_FOUND", "Outward request not found"), request());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("OUTWARD_NOT_FOUND", response.getBody().code());
    }

    @Test
    void mapsAuthorizationTo403() {
        ResponseEntity<ApiError> response = handler.handleOutward(
                new OutwardException("OUTWARD_APPROVAL_ROLE_NOT_ALLOWED", "Approval role not allowed"), request());
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void mapsDownstreamFailureTo503() {
        ResponseEntity<ApiError> response = handler.handleDownstream(
                new DownstreamServiceException("MASTER_DATA_SERVICE_UNAVAILABLE", "Master Data unavailable", null), request());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void mapsBusinessValidationTo400() {
        ResponseEntity<ApiError> response = handler.handleMasterDataValidation(
                new MasterDataValidationException("OUTWARD_ITEM_INACTIVE", "Item is inactive"), request());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private HttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/outward");
        request.addHeader("X-Trace-Id", "trace-123");
        return request;
    }
}
