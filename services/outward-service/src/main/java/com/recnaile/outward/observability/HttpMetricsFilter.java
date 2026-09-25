package com.recnaile.outward.observability;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class HttpMetricsFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    public HttpMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        String route = route(request.getRequestURI());
        String method = request.getMethod();
        try {
            filterChain.doFilter(request, response);
        } finally {
            String status = Integer.toString(response.getStatus());
            Counter.builder("outward_http_requests")
                    .tag("method", method).tag("route", route).tag("status", status)
                    .register(meterRegistry).increment();
            Timer.builder("outward_http_request_duration")
                    .tag("method", method).tag("route", route).tag("status", status)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    private String route(String uri) {
        if (uri == null) return "UNKNOWN";
        if (uri.startsWith("/api/v1/outward")) return "/api/v1/outward";
        if (uri.startsWith("/actuator/")) return "/actuator/*";
        if (uri.startsWith("/swagger-ui")) return "/swagger-ui/*";
        if (uri.startsWith("/v3/api-docs")) return "/v3/api-docs";
        return "/other";
    }
}
