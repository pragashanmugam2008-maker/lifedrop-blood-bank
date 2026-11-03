package com.lifedrop.controller;

import com.lifedrop.dto.request.LoginRequest;
import com.lifedrop.dto.request.RegisterRequest;
import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.AuthResponse;
import com.lifedrop.dto.response.UserResponse;
import com.lifedrop.security.UserPrincipal;
import com.lifedrop.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, and profile retrieval")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user (Donor or Patient)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody RegisterRequest request) {
        String corrId = (correlationId != null && !correlationId.trim().isEmpty())
                ? correlationId.trim()
                : "corr-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        AuthResponse response = authService.register(request, corrId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Correlation-ID", corrId)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody LoginRequest request) {
        String corrId = (correlationId != null && !correlationId.trim().isEmpty())
                ? correlationId.trim()
                : "corr-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        AuthResponse response = authService.login(request, corrId);
        return ResponseEntity.ok()
                .header("X-Correlation-ID", corrId)
                .body(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in"));
        }
        UserResponse user = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
