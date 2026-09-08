package com.recnaile.auth.controller;

import com.recnaile.auth.dto.LoginRequest;
import com.recnaile.auth.dto.LoginResponse;
import com.recnaile.auth.dto.RefreshTokenRequest;
import com.recnaile.auth.dto.RegisterRequest;
import com.recnaile.auth.dto.RegisterResponse;
import com.recnaile.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

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
        description = "Creates a new employee account."
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
            description = "Email already registered"
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
            "Authenticates a user and returns access and refresh tokens."
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
            "Returns information about the currently authenticated user."
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
    // UNLOCK ACCOUNT
    // =========================================================

    @Operation(
        summary = "Unlock user account",
        description =
            "Unlocks a locked user account. Only MD can perform this operation."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Account unlocked successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid user ID or user not found"
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
    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<String> unlockAccount(
        @PathVariable String userId
    ) {

        authService.unlockAccount(userId);

        return ResponseEntity.ok(
            "Account unlocked successfully"
        );
    }
}
