package com.recnaile.inward;

import com.recnaile.inward.dto.ApiErrorResponse;
import com.recnaile.inward.exception.GRNExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GRNExceptionHandlerTest {

    @Test
    void invalidItemReturnsBadRequestWithStableErrorCode() {
        GRNExceptionHandler handler = new GRNExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn("corr-123");

        var response = handler.handleBadRequest(
                new IllegalArgumentException("Item Item-999 does not belong to PO PO-008"),
                request
        );

        assertEquals(400, response.getStatusCode().value());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.success());
        assertNull(body.data());
        assertEquals("ITEM_NOT_IN_PURCHASE_ORDER", body.error().code());
        assertEquals("corr-123", body.error().traceId());
    }
}
