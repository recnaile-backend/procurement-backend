package com.recnaile.reporting.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice
public class ReportingExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class) ProblemDetail badRequest(IllegalArgumentException ex){ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,ex.getMessage());p.setType(URI.create("about:blank"));p.setTitle("Invalid reporting request");return p;}
}
