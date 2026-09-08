package com.recnaile.auth.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/rbac")
@SecurityRequirement(name = "bearerAuth")
public class RbacController {

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<String> employee() {
        return ResponseEntity.ok("Employee access granted");
    }

    @GetMapping("/procurement")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public ResponseEntity<String> procurement() {
        return ResponseEntity.ok("Procurement access granted");
    }

    @GetMapping("/tl")
    @PreAuthorize("hasRole('TL')")
    public ResponseEntity<String> tl() {
        return ResponseEntity.ok("TL access granted");
    }

    @GetMapping("/md")
    @PreAuthorize("hasRole('MD')")
    public ResponseEntity<String> md() {
        return ResponseEntity.ok("MD access granted");
    }
}
