package com.recnaile.outward.controller;

import com.recnaile.outward.dto.CreateOutwardRequest;
import com.recnaile.outward.dto.OutwardResponse;
import com.recnaile.outward.service.OutwardRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import com.recnaile.outward.dto.ApiError;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Outward Requests", description = "Create outward material requests.")
@SecurityRequirement(name = "bearerAuth")
public class OutwardRequestController {

    private final OutwardRequestService service;

    public OutwardRequestController(OutwardRequestService service) {
        this.service = service;
    }

    @Operation(
            summary = "Create an outward request",
            description = "Creates a DRAFT outward request. Item, department, project and stock validation is performed against Master Data before persistence.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Outward request created", content = @Content(schema = @Schema(implementation = OutwardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation or Master Data business validation failure", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Master Data service unavailable", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<OutwardResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Outward request details", required = true,
                    content = @Content(schema = @Schema(implementation = CreateOutwardRequest.class)))
            @Valid @RequestBody CreateOutwardRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.<OutwardResponse>status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.<OutwardResponse>status(HttpStatus.CREATED)
                .body(service.create(request, authentication.getName(), authentication));
    }
}
