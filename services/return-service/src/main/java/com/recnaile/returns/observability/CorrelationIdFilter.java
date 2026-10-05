package com.recnaile.returns.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final int MAX_LENGTH = 100;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = normalize(request.getHeader(HEADER_NAME));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_LENGTH) {
            return null;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isISOControl(c)) {
                return null;
            }
        }
        return normalized;
    }
}
