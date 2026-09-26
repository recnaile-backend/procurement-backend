package com.recnaile.outward.controller;

import com.recnaile.outward.dto.CreateOutwardRequest;
import com.recnaile.outward.dto.OutwardResponse;
import com.recnaile.outward.service.OutwardRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outward")
public class OutwardRequestController {

    private final OutwardRequestService service;

    public OutwardRequestController(OutwardRequestService service) {
        this.service = service;
    }

    @Operation(summary = "Create an outward request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Outward request created"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ResponseEntity<OutwardResponse> create(
            @Valid @RequestBody CreateOutwardRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.<OutwardResponse>status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.<OutwardResponse>status(HttpStatus.CREATED)
                .body(service.create(request, authentication.getName()));
    }
}
