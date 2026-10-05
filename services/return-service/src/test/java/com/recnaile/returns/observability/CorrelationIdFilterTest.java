package com.recnaile.returns.observability;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void cleanup() {
        org.slf4j.MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    void preservesValidIncomingCorrelationIdAndReturnsIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("corr-123", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        verify(chain).doFilter(request, response);
        assertNull(org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
        verify(chain).doFilter(request, response);
    }

    @Test
    void replacesOversizedIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "x".repeat(101));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotEquals("x".repeat(101), correlationId);
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
    }
}
