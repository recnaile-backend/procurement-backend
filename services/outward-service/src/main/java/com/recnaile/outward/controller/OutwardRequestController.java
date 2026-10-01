package com.recnaile.outward.controller;

import com.recnaile.outward.dto.CreateOutwardRequest;
import com.recnaile.outward.dto.OutwardResponse;
import com.recnaile.outward.dto.IssuedReturnValidation;
import com.recnaile.outward.service.OutwardRequestService;
import com.recnaile.outward.service.OutwardIssueService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/outward")
@Tag(name = "Outward Requests", description = "Create outward material requests.")
@SecurityRequirement(name = "bearerAuth")
public class OutwardRequestController {

    private final OutwardRequestService service;
    private final OutwardIssueService issueService;

    public OutwardRequestController(OutwardRequestService service, OutwardIssueService issueService) {
        this.service = service;
        this.issueService = issueService;
    }

    @Operation(
            summary = "Get all outward requests",
            description = "Returns all outward requests ordered by requested time descending. Access is authenticated; MD has full access, while TL approval/rejection is restricted to requests from the same department.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Outward requests retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<OutwardResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @Operation(
            summary = "Validate an issued outward item for return",
            description = "Returns the issued quantity for an item only when the outward request has reached ISSUED status. This endpoint is intended for the Return Service synchronous validation boundary.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issued item validated", content = @Content(schema = @Schema(implementation = com.recnaile.outward.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Outward is not issued or item is not part of the issue transaction", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Outward request or issue transaction not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/return-validation")
    public ResponseEntity<com.recnaile.outward.dto.ApiResponse<IssuedReturnValidation>> validateReturn(
            @RequestParam String outwardId,
            @RequestParam String itemId) {
        return ResponseEntity.ok(com.recnaile.outward.dto.ApiResponse.success(
                issueService.validateReturn(OutwardNumberPath.normalize(outwardId), itemId)));
    }

    @Operation(
            summary = "Create an outward request",
            description = "Creates and immediately submits an outward request for approval. Item, department, project and stock validation is performed against Master Data before persistence.")
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
