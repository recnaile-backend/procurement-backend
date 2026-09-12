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

import com.recnaile.masterdata.dto.department.CreateDepartmentRequest;
import com.recnaile.masterdata.dto.department.DepartmentResponse;
import com.recnaile.masterdata.dto.department.UpdateDepartmentRequest;
import com.recnaile.masterdata.service.DepartmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/master-data/departments")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "Create department")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Department created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid department data"
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
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PostMapping
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody CreateDepartmentRequest request) {

        DepartmentResponse response = departmentService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Multi-add departments")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Departments created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid departments data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have the required role")
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PostMapping("/multi-add")
    public ResponseEntity<List<DepartmentResponse>> multiAdd(
            @Valid @RequestBody List<@Valid CreateDepartmentRequest> requests) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(departmentService.multiAdd(requests));
    }

    @Operation(summary = "Get all departments")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Departments retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAll() {

        return ResponseEntity.ok(
                departmentService.getAll()
        );
    }

    @Operation(summary = "Get department by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Department retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Department not found"
            )
    })
    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> getById(
            @PathVariable String departmentId) {

        return ResponseEntity.ok(
                departmentService.getById(departmentId)
        );
    }

    @Operation(summary = "Update department")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Department updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid department data"
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
                    description = "Department not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Department version conflict"
            )
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> update(
            @PathVariable String departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request) {

        return ResponseEntity.ok(
                departmentService.update(departmentId, request)
        );
    }

    @Operation(summary = "Deactivate department")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Department deactivated successfully"
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
                    description = "Department not found"
            )
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PostMapping("/{departmentId}/deactivate")
    public ResponseEntity<DepartmentResponse> deactivate(
            @PathVariable String departmentId) {

        return ResponseEntity.ok(
                departmentService.deactivate(departmentId)
        );
    }

    @Operation(summary = "Delete department by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Department deleted successfully"
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
                    description = "Department not found"
            )
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @DeleteMapping("/{departmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String departmentId) {

        departmentService.delete(departmentId);

        return ResponseEntity.noContent().build();
    }
}