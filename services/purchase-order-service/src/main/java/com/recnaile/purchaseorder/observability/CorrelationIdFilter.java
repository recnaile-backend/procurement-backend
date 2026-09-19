package com.recnaile.purchaseorder.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    /** Backward-compatible alias used by the global exception handler. */
    public static final String HEADER = CORRELATION_HEADER;
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final Tracer tracer;

    public CorrelationIdFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = firstNonBlank(request.getHeader(CORRELATION_HEADER), UUID.randomUUID().toString());
        String requestId = firstNonBlank(request.getHeader(REQUEST_ID_HEADER), UUID.randomUUID().toString());

        MDC.put("correlationId", correlationId);
        MDC.put("requestId", requestId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        putCurrentTraceContext();
        putAuthenticatedUser();

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("requestId");
            MDC.remove("traceId");
            MDC.remove("spanId");
            MDC.remove("userId");
            MDC.remove("eventId");
        }
    }

    public static void putEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            MDC.remove("eventId");
        } else {
            MDC.put("eventId", eventId);
        }
    }

    public static void putCurrentTraceContext(Tracer tracer) {
        if (tracer == null) {
            return;
        }
        Span span = tracer.currentSpan();
        if (span != null) {
            MDC.put("traceId", span.context().traceId());
            MDC.put("spanId", span.context().spanId());
        }
    }

    private void putCurrentTraceContext() {
        putCurrentTraceContext(tracer);
    }

    private void putAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !authentication.getName().isBlank()) {
            MDC.put("userId", authentication.getName());
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
