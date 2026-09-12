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

import com.recnaile.masterdata.dto.project.CreateProjectRequest;
import com.recnaile.masterdata.dto.project.ProjectResponse;
import com.recnaile.masterdata.dto.project.UpdateProjectRequest;
import com.recnaile.masterdata.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/master-data/projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "Create project")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Project created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid project data"
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
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request) {

        ProjectResponse response = projectService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Multi-add projects")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projects created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid projects data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have the required role")
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PostMapping("/multi-add")
    public ResponseEntity<List<ProjectResponse>> multiAdd(
            @Valid @RequestBody List<@Valid CreateProjectRequest> requests) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(projectService.multiAdd(requests));
    }

    @Operation(summary = "Get all projects")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Projects retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll() {

        return ResponseEntity.ok(
                projectService.getAll()
        );
    }

    @Operation(summary = "Get project by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Project retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project not found"
            )
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getById(
            @PathVariable String projectId) {

        return ResponseEntity.ok(
                projectService.getById(projectId)
        );
    }

    @Operation(summary = "Update project")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Project updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid project data"
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
                    description = "Project not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Project version conflict"
            )
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable String projectId,
            @Valid @RequestBody UpdateProjectRequest request) {

        return ResponseEntity.ok(
                projectService.update(projectId, request)
        );
    }

    @Operation(summary = "Deactivate project")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Project deactivated successfully"
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
                    description = "Project not found"
            )
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PostMapping("/{projectId}/deactivate")
    public ResponseEntity<ProjectResponse> deactivate(
            @PathVariable String projectId) {

        return ResponseEntity.ok(
                projectService.deactivate(projectId)
        );
    }

    @Operation(summary = "Delete project by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Project deleted successfully"
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
                    description = "Project not found"
            )
    })
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(
            @PathVariable String projectId) {

        projectService.delete(projectId);

        return ResponseEntity.noContent().build();
    }
}