package com.recnaile.outward.controller;

import java.util.List;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.dto.request.CancelOutwardRequest;
import com.recnaile.outward.dto.request.CreateOutwardRequest;
import com.recnaile.outward.dto.request.RejectOutwardRequest;
import com.recnaile.outward.dto.response.ApiResponse;
import com.recnaile.outward.dto.response.OutwardResponse;
import com.recnaile.outward.service.OutwardApplicationService;
import com.recnaile.outward.service.OutwardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/outward")
@Tag(name = "Outward", description = "Stock issue requests, approval and issue transactions")
@SecurityRequirement(name = "bearerAuth")
public class OutwardController {

    private final OutwardApplicationService applicationService;
    private final OutwardQueryService queryService;

    public OutwardController(OutwardApplicationService applicationService,
                             OutwardQueryService queryService) {
        this.applicationService = applicationService;
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Create outward issue request")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Outward request created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency or state conflict"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Master-data service unavailable")
    })
    public ResponseEntity<ApiResponse<OutwardResponse>> create(
            @Valid @RequestBody CreateOutwardRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        OutwardResponse response = applicationService.create(
                request, authentication.getName(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List outward requests")
    public ResponseEntity<ApiResponse<List<OutwardResponse>>> list(
            @RequestParam(required = false) OutwardStatus status,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        boolean employee = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EMPLOYEE".equals(a.getAuthority()));
        List<OutwardResponse> data;
        if (employee) {
            data = queryService.mine(authentication.getName(), limit);
        } else {
            data = status == null
                    ? queryService.all(limit)
                    : queryService.byStatus(status, limit);
        }
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/mine")
    @Operation(summary = "List my outward requests")
    public ResponseEntity<ApiResponse<List<OutwardResponse>>> mine(
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                queryService.mine(authentication.getName(), limit)));
    }

    @GetMapping("/{outwardId}")
    @Operation(summary = "Get outward request")
    public ResponseEntity<ApiResponse<OutwardResponse>> get(@PathVariable String outwardId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(queryService.get(outwardId, authentication)));
    }

    @PostMapping("/{outwardId}/approve")
    @Operation(summary = "Approve and issue outward request")
    public ResponseEntity<ApiResponse<OutwardResponse>> approve(
            @PathVariable String outwardId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("UNKNOWN");
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.approve(outwardId, authentication.getName(), role, idempotencyKey)));
    }

    @PostMapping("/{outwardId}/reject")
    @Operation(summary = "Reject outward request")
    public ResponseEntity<ApiResponse<OutwardResponse>> reject(
            @PathVariable String outwardId,
            @Valid @RequestBody RejectOutwardRequest request,
            Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("UNKNOWN");
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.reject(outwardId, authentication.getName(), role, request)));
    }

    @PostMapping("/{outwardId}/cancel")
    @Operation(summary = "Cancel outward request")
    public ResponseEntity<ApiResponse<OutwardResponse>> cancel(
            @PathVariable String outwardId,
            @RequestBody(required = false) CancelOutwardRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.cancel(outwardId, authentication.getName(), request)));
    }
}
