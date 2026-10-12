package com.recnaile.reporting.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice
public class ReportingExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {
        return problem(ex.getMessage());
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class})
    ProblemDetail invalidParameter(Exception ex) {
        return problem("Invalid reporting request parameter: " + ex.getMessage());
    }

    private ProblemDetail problem(String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail == null ? "Invalid reporting request" : detail);
        p.setType(URI.create("about:blank"));
        p.setTitle("Invalid reporting request");
        return p;
    }
}
