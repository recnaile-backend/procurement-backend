package com.recnaile.outward.observability;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);

        String traceparent = request.getHeader("traceparent");
        if (StringUtils.hasText(traceparent)) {
            String[] parts = traceparent.split("-");
            if (parts.length == 4 && parts[1].matches("[0-9a-fA-F]{32}")) {
                MDC.put("traceId", parts[1]);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("traceId");
        }
    }
}
