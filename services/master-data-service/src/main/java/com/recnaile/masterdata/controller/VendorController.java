package com.recnaile.masterdata.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recnaile.masterdata.dto.vendor.CreateVendorRequest;
import com.recnaile.masterdata.dto.vendor.UpdateVendorRequest;
import com.recnaile.masterdata.dto.vendor.VendorResponse;
import com.recnaile.masterdata.service.VendorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/master-data/vendors")
@SecurityRequirement(name = "bearerAuth")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @Operation(summary = "Create vendor")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Vendor created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid vendor data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PostMapping
    public ResponseEntity<VendorResponse> create(
            @Valid @RequestBody CreateVendorRequest request) {

        VendorResponse response = vendorService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Multi-add vendors")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vendors created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid vendors data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have the required role")
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PostMapping("/multi-add")
    public ResponseEntity<List<VendorResponse>> multiAdd(
            @Valid @RequestBody List<@Valid CreateVendorRequest> requests) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(vendorService.multiAdd(requests));
    }

    @Operation(summary = "Get all vendors")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vendors retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAll() {

        return ResponseEntity.ok(
                vendorService.getAll()
        );
    }

    @Operation(summary = "Get vendor by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vendor retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vendor not found"
            )
    })
    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> getById(
            @PathVariable String vendorId) {

        return ResponseEntity.ok(
                vendorService.getById(vendorId)
        );
    }

    @Operation(summary = "Update vendor")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vendor updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid vendor data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vendor not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Vendor version conflict"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PutMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> update(
            @PathVariable String vendorId,
            @Valid @RequestBody UpdateVendorRequest request) {

        return ResponseEntity.ok(
                vendorService.update(vendorId, request)
        );
    }

    @Operation(summary = "Deactivate vendor")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vendor deactivated successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vendor not found"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PostMapping("/{vendorId}/deactivate")
    public ResponseEntity<VendorResponse> deactivate(
            @PathVariable String vendorId) {

        return ResponseEntity.ok(
                vendorService.deactivate(vendorId)
        );
    }

    @Operation(summary = "Delete vendor by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Vendor deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vendor not found"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @DeleteMapping("/{vendorId}")
    public ResponseEntity<Void> delete(
            @PathVariable String vendorId) {

        vendorService.delete(vendorId);

        return ResponseEntity.noContent().build();
    }
}