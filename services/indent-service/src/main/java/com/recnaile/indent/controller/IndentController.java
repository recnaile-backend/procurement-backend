package com.recnaile.indent.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recnaile.indent.dto.request.ApproveIndentRequest;
import com.recnaile.indent.dto.request.CreateIndentRequest;
import com.recnaile.indent.dto.request.RejectIndentRequest;
import com.recnaile.indent.dto.request.VerifyIndentRequest;
import com.recnaile.indent.dto.response.ApiResponse;
import com.recnaile.indent.dto.response.AutoVerifyResponse;
import com.recnaile.indent.dto.response.IndentDetailsResponse;
import com.recnaile.indent.dto.response.IndentListItemResponse;
import com.recnaile.indent.dto.response.IndentSummaryResponse;
import com.recnaile.indent.service.IndentApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/indents")
@Tag(
        name = "Indents",
        description = "Procurement indent lifecycle APIs including creation, verification, approval, rejection and retrieval"
)
@SecurityRequirement(name = "bearerAuth")
public class IndentController {

    private final IndentApplicationService indentApplicationService;

    public IndentController(
            IndentApplicationService indentApplicationService) {
        this.indentApplicationService = indentApplicationService;
    }


    // =========================================================================
    // AUTO VERIFY ITEMS AGAINST MASTER DATA
    // =========================================================================

    @PostMapping("/{indentNumber}/auto-verify")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Auto verify indent items against Master Data",
            description = """
                    Checks every item in the specified indent against the
                    Master Data Item Master list.

                    The endpoint is available to every authenticated role.

                    For each item:
                    - AVAILABLE: the item exists as an active Item Master
                      record.
                    - NOT_AVAILABLE: the item does not exist in Item Master
                      or its Item Master record is inactive.

                    This API verifies item existence/active status only. It
                    does not change the main indent workflow status.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Item Master verification completed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Indent was not found"
            ),
    })
    public ResponseEntity<ApiResponse<AutoVerifyResponse>> autoVerifyIndent(

            @Parameter(
                    description = "Unique indent number. Enter the literal encoded business value, for example IN%2F26-27%2F004.",
                    required = true,
                    example = "IN%2F26-27%2F004"
            )
            @PathVariable("indentNumber")
            String indentNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        indentApplicationService.autoVerifyInventory(
                                indentNumber
                        )
                )
        );
    }


    // =========================================================================
    // GET ALL INDENTS
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','PROCUREMENT','TL','MD')")
    @Operation(
            summary = "Get all indents",
            description = """
                    Returns all procurement indents.

                    An optional departmentName can be supplied to filter
                    indents by department.

                    Results are returned newest first.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Indents retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to access indents"
            )
    })
    public ResponseEntity<ApiResponse<Page<IndentListItemResponse>>> listIndents(

            @Parameter(
                    description = "Department name used to filter indents. Leave empty to retrieve indents from all departments.",
                    example = "IT Department"
            )
            @RequestParam(required = false)
            String departmentName) {

        Pageable pageable =
                Pageable.unpaged(
                        Sort.by(
                                Sort.Order.desc("createdAt")
                        )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        indentApplicationService.list(
                                null,
                                departmentName,
                                pageable
                        )
                )
        );
    }

    // =========================================================================
    // GET INDENT BY INDENT NUMBER
    // =========================================================================

    @GetMapping("/{indentNumber}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get indent details",
            description = """
                    Retrieves complete details of a procurement indent
                    using its unique identifier.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Indent details retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Indent was not found"
            )
    })
    public ResponseEntity<ApiResponse<IndentDetailsResponse>> getIndent(

            @Parameter(
                    description = "Unique indent number. Use the encoded indent number, for example IN%2F26-27%2F002.",
                    required = true,
                    example = "IN%2F26-27%2F002"
            )
            @PathVariable("indentNumber")
            String indentNumber) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        indentApplicationService.getDetails(indentNumber)
                )
        );
    }


    // =========================================================================
    // VERIFY INDENT
    // =========================================================================

    @PostMapping("/{indentNumber}/verify")
    @PreAuthorize("hasRole('PROCUREMENT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Verify an indent",
            description = """
                    Verifies a submitted procurement indent.

                    Only users with the PROCUREMENT role can perform
                    verification.

                    Successful verification moves the indent through:

                    SUBMITTED -> UNDER_VERIFICATION -> VERIFIED
                    -> PENDING_APPROVAL
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Indent verified successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid verification request or invalid indent state"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Only PROCUREMENT users can verify indents"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Indent was not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Indent state changed concurrently"
            )
    })
    public ResponseEntity<ApiResponse<IndentSummaryResponse>> verifyIndent(

            @Parameter(
                    description = "Unique indent number. Use the encoded indent number, for example IN%2F26-27%2F002.",
                    required = true,
                    example = "IN%2F26-27%2F002"
            )
            @PathVariable("indentNumber")
            String indentNumber,

            @RequestBody(required = false)
            VerifyIndentRequest request,

            Authentication authentication) {

        String remarks =
                request == null
                        ? null
                        : request.remarks();

        IndentSummaryResponse response =
                indentApplicationService.verify(
                        indentNumber,
                        authentication.getName(),
                        remarks
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }


    // =========================================================================
    // APPROVE INDENT
    // =========================================================================

    @PostMapping("/{indentNumber}/approve")
    @PreAuthorize("hasAnyRole('TL','MD')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Approve an indent",
            description = """
                    Approves an indent that has completed verification.

                    Approval is available only to TL or MD users according
                    to the configured approval routing.

                    Successful approval moves the indent through:

                    PENDING_APPROVAL -> APPROVED -> READY_FOR_PO

                    The optional Idempotency-Key header makes approval processing safe to retry
                    without applying the approval more than once. It is not the indent ID or indent number.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Indent approved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid approval request or invalid indent state"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User does not have the required TL or MD role"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Indent was not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Indent was already processed or was modified concurrently"
            )
    })
    public ResponseEntity<ApiResponse<IndentSummaryResponse>> approveIndent(

            @Parameter(
                    description = "Unique indent number. Use the encoded indent number, for example IN%2F26-27%2F002.",
                    required = true,
                    example = "IN%2F26-27%2F002"
            )
            @PathVariable("indentNumber")
            String indentNumber,

            @RequestBody(required = false)
            ApproveIndentRequest request,

            Authentication authentication,

            @Parameter(
                    description = "Unique key used to make approval processing idempotent",
                    example = "approval-IND-10001-001"
            )
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey) {

        String approvalRole =
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .filter(authority ->
                                "ROLE_TL".equals(authority)
                                        || "ROLE_MD".equals(authority)
                        )
                        .map(authority ->
                                authority.substring("ROLE_".length())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new org.springframework.security.access.AccessDeniedException(
                                        "Approval role is required"
                                )
                        );

        IndentSummaryResponse response =
                indentApplicationService.approve(
                        indentNumber,
                        authentication.getName(),
                        approvalRole,
                        request,
                        idempotencyKey
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }


    // =========================================================================
    // REJECT INDENT
    // =========================================================================

    @PostMapping("/{indentNumber}/reject")
    @PreAuthorize("hasAnyRole('TL','MD')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Reject an indent",
            description = """
                    Rejects an indent during the approval workflow.

                    Rejection requires a reason and records the rejecting
                    user's identity and timestamp.

                    The optional Idempotency-Key header prevents duplicate
                    rejection processing.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Indent rejected successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid rejection request or invalid indent state"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User does not have the required TL or MD role"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Indent was not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Indent was already processed or was modified concurrently"
            )
    })
    public ResponseEntity<ApiResponse<IndentSummaryResponse>> rejectIndent(

            @Parameter(
                    description = "Unique indent number. Use the encoded indent number, for example IN%2F26-27%2F002.",
                    required = true,
                    example = "IN%2F26-27%2F002"
            )
            @PathVariable("indentNumber")
            String indentNumber,

            @Valid
            @RequestBody
            RejectIndentRequest request,

            Authentication authentication,

            @Parameter(
                    description = "Unique key used to make rejection processing idempotent",
                    example = "rejection-IND-10001-001"
            )
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey) {

        IndentSummaryResponse response =
                indentApplicationService.reject(
                        indentNumber,
                        authentication.getName(),
                        request,
                        idempotencyKey
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }


    // =========================================================================
    // CREATE / SUBMIT INDENT
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create and submit an indent",
            description = """
                    Creates a new procurement indent and submits it for
                    verification.

                    The authenticated EMPLOYEE becomes the requester.

                    The request must contain the required department,
                    project, reason and item information.

                    Inventory availability is NOT checked during creation.
                    The indent is submitted regardless of whether the requested
                    quantity is currently available in inventory.

                    Inventory availability can be checked later using the
                    Auto Verify API.

                    Initial workflow transition:

                    DRAFT -> SUBMITTED
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Indent created and submitted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid indent request or validation failure"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Only EMPLOYEE users can create indents"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Duplicate request or conflicting indent creation"
            )
    })
    public ResponseEntity<ApiResponse<IndentSummaryResponse>> createIndent(

            @Valid
            @RequestBody
            CreateIndentRequest request,

            Authentication authentication) {

        IndentSummaryResponse response =
                indentApplicationService.createAndSubmit(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(response)
                );
    }
}