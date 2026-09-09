package com.recnaile.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recnaile.auth.dto.AddTeamMemberRequest;
import com.recnaile.auth.dto.LoginRequest;
import com.recnaile.auth.dto.LoginResponse;
import com.recnaile.auth.dto.RefreshTokenRequest;
import com.recnaile.auth.dto.RegisterRequest;
import com.recnaile.auth.dto.RegisterResponse;
import com.recnaile.auth.dto.TeamMemberResponse;
import com.recnaile.auth.dto.TeamMemberUpdateRequest;
import com.recnaile.auth.dto.UpdateEmployeeRequest;
import com.recnaile.auth.dto.UpdateEmployeeRoleRequest;
import com.recnaile.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    // =========================================================
    // REGISTER
    // =========================================================

    @Operation(
        summary = "Register a new user",
        description =
            "Creates a new employee account using employee ID. " +
            "Every new user is automatically assigned the EMPLOYEE role."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid registration request"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Employee ID already registered"
        )
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
        @Valid @RequestBody RegisterRequest request
    ) {

        RegisterResponse response =
            authService.register(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }


    // =========================================================
    // LOGIN
    // =========================================================

    @Operation(
        summary = "Login",
        description =
            "Authenticates a user using employee ID and password " +
            "and returns access and refresh tokens."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login successful"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid credentials, disabled account, or locked account"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response =
            authService.login(request);

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    @Operation(
        summary = "Refresh access token",
        description =
            "Uses a valid refresh token to issue new access and refresh tokens."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tokens refreshed successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid, expired, or revoked refresh token"
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
        @Valid @RequestBody RefreshTokenRequest request
    ) {

        LoginResponse response =
            authService.refresh(request);

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @Operation(
        summary = "Logout",
        description =
            "Revokes the supplied refresh token so it can no longer be used."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid or already revoked refresh token"
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
        @Valid @RequestBody RefreshTokenRequest request
    ) {

        authService.logout(request);

        return ResponseEntity.ok(
            "Logout successful"
        );
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    @Operation(
        summary = "Get authenticated user",
        description =
            "Returns the identity from the currently authenticated access token."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Authenticated successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<String> me(
        Authentication authentication
    ) {

        return ResponseEntity.ok(
            "Authenticated as: " +
                authentication.getName()
        );
    }


    // =========================================================
    // GET ALL EMPLOYEES
    // =========================================================

    @Operation(
        summary = "Get all employees",
        description =
            "Returns all employee accounts. " +
            "Only MD can access this operation."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Employees retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        ),
        @ApiResponse(
            responseCode = "403",
            description =
                "User does not have MD role"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('MD')")
    @GetMapping("/users")
    public ResponseEntity<List<RegisterResponse>> getAllEmployees() {

        List<RegisterResponse> employees =
            authService.getAllEmployees();

        return ResponseEntity.ok(employees);
    }


    // =========================================================
    // DELETE EMPLOYEE
    // =========================================================

    @Operation(
        summary = "Delete employee",
        description =
            "Deletes an employee account using employee ID. " +
            "Only MD can perform this operation."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Employee deleted successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid employee ID, employee not found, or MD account"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        ),
        @ApiResponse(
            responseCode = "403",
            description =
                "User does not have MD role"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('MD')")
    @DeleteMapping("/users/{employeeId}")
    public ResponseEntity<String> deleteEmployee(
        @PathVariable String employeeId
    ) {

        authService.deleteEmployee(employeeId);

        return ResponseEntity.ok(
            "Employee deleted successfully"
        );
    }


    // =========================================================
    // UPDATE EMPLOYEE ROLE
    // =========================================================

    @Operation(
        summary = "Update employee role",
        description =
            "Changes an employee's role using employee ID. " +
            "Only MD can perform this operation. " +
            "The MD role cannot be assigned through this API."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Employee role updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid role, employee not found, or MD account"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        ),
        @ApiResponse(
            responseCode = "403",
            description =
                "User does not have MD role"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('MD')")
    @PutMapping("/users/{employeeId}/role")
    public ResponseEntity<String> updateEmployeeRole(
        @PathVariable String employeeId,
        @Valid @RequestBody UpdateEmployeeRoleRequest request
    ) {

        authService.updateEmployeeRole(
            employeeId,
            request.role()
        );

        return ResponseEntity.ok(
            "Employee role updated successfully"
        );
    }


    // =========================================================
    // UPDATE EMPLOYEE DETAILS
    // =========================================================

    @Operation(
        summary = "Update employee details",
        description =
            "Updates an employee's profile information using employee ID. " +
            "Only MD can perform this operation."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description =
                "Employee details updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid details, duplicate employee ID, " +
                "employee not found, or MD account"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        ),
        @ApiResponse(
            responseCode = "403",
            description =
                "User does not have MD role"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('MD')")
    @PutMapping("/users/{employeeId}")
    public ResponseEntity<String> updateEmployeeDetails(
        @PathVariable String employeeId,
        @Valid @RequestBody UpdateEmployeeRequest request
    ) {

        authService.updateEmployeeDetails(
            employeeId,
            request.employeeId(),
            request.firstName(),
            request.lastName(),
            request.departmentName()
        );

        return ResponseEntity.ok(
            "Employee details updated successfully"
        );
    }


    // =========================================================
    // UNLOCK ACCOUNT
    // =========================================================

    @Operation(
        summary = "Unlock user account",
        description =
            "Unlocks an employee account using employee ID. " +
            "Only MD can perform this operation."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Account unlocked successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid employee ID or employee not found"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        ),
        @ApiResponse(
            responseCode = "403",
            description =
                "User does not have MD role"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('MD')")
    @PutMapping("/users/{employeeId}/unlock")
    public ResponseEntity<String> unlockAccount(
        @PathVariable String employeeId
    ) {

        authService.unlockAccount(employeeId);

        return ResponseEntity.ok(
            "Account unlocked successfully"
        );
    }


    // =========================================================
    // TEAM MANAGEMENT - TL + MD
    // =========================================================

    // =========================================================
    // GET MY TEAM
    // =========================================================

    @Operation(
        summary = "Get my team",
        description =
            "Returns team employees using employee IDs. " +
            "TL can view employees assigned to that TL. " +
            "MD can access team information."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description =
                "Team retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        ),
        @ApiResponse(
            responseCode = "403",
            description =
                "User does not have TL or MD role"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @GetMapping("/team")
    public ResponseEntity<List<TeamMemberResponse>> getMyTeam(
        Authentication authentication
    ) {

        return ResponseEntity.ok(
            authService.getMyTeam(authentication)
        );
    }


    // =========================================================
    // ADD TEAM MEMBER
    // =========================================================

    @Operation(
        summary = "Add employee to a team",
        description =
            "Adds an existing employee to a team using employee ID. " +
            "Both TL and MD are allowed to perform this operation."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description =
                "Employee added to team successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid employee or employee cannot be added"
        ),
        @ApiResponse(
            responseCode = "401",
            description =
                "Missing, expired, or invalid access token"
        ),
        @ApiResponse(
            responseCode = "403",
            description =
                "User does not have TL or MD role, " +
                "or employee cannot be assigned to the team"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('TL', 'MD')")
    @PostMapping("/team/members")
    public ResponseEntity<TeamMemberResponse> addTeamMember(
        @Valid @RequestBody AddTeamMemberRequest request,
        Authentication authentication
    ) {

        return ResponseEntity.ok(
            authService.addTeamMember(
                request,
                authentication
            )
        );
    }


// =========================================================
// UPDATE TEAM MEMBER
// =========================================================

@Operation(
    summary = "Update employee in team",
    description =
        "Updates an employee in the selected Team Leader's team using employee ID. " +
        "TL and MD are allowed."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Employee updated successfully"
    ),
    @ApiResponse(
        responseCode = "401",
        description = "Missing, expired, or invalid access token"
    ),
    @ApiResponse(
        responseCode = "403",
        description =
            "Employee does not belong to the specified Team Leader's team"
    )
})
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TL', 'MD')")
@PutMapping("/team/members/{employeeId}")
public ResponseEntity<TeamMemberResponse> updateTeamMember(
    @PathVariable String employeeId,
    @Valid @RequestBody TeamMemberUpdateRequest request,
    Authentication authentication
) {

    return ResponseEntity.ok(
        authService.updateTeamMember(
            employeeId,
            request.teamLeaderEmployeeId(),
            request,
            authentication
        )
    );
}
// =========================================================
// REMOVE TEAM MEMBER
// =========================================================

@Operation(
    summary = "Remove employee from team",
    description =
        "Removes an employee from the specified Team Leader's team using employee ID. " +
        "The employee account is not deleted. TL and MD are allowed."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Employee removed from team successfully"
    ),
    @ApiResponse(
        responseCode = "401",
        description = "Missing, expired, or invalid access token"
    ),
    @ApiResponse(
        responseCode = "403",
        description =
            "Employee does not belong to the specified Team Leader's team"
    )
})
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TL', 'MD')")
@DeleteMapping("/team/members/{employeeId}")
public ResponseEntity<String> removeTeamMember(
    @PathVariable String employeeId,
    @RequestParam String teamLeaderEmployeeId,
    Authentication authentication
) {

    authService.removeTeamMember(
        employeeId,
        teamLeaderEmployeeId,
        authentication
    );

    return ResponseEntity.ok(
        "Employee removed from team successfully"
    );
}
}