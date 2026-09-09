package com.recnaile.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.auth.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class SecurityExceptionHandlers
    implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityExceptionHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {

        writeErrorResponse(
            response,
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "Authentication is required to access this resource",
            request.getRequestURI()
        );
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {

        writeErrorResponse(
            response,
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "You do not have permission to access this resource",
            request.getRequestURI()
        );
    }

    private void writeErrorResponse(
        HttpServletResponse response,
        HttpStatus status,
        String error,
        String message,
        String path
    ) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            status.value(),
            error,
            message,
            path
        );

        response.setStatus(status.value());
        response.setContentType("application/json");

        objectMapper.writeValue(
            response.getOutputStream(),
            errorResponse
        );
    }
}
