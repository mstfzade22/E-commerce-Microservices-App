package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegistrationRequest;
import com.ecommerce.userservice.dto.response.LoginResponse;
import com.ecommerce.userservice.dto.response.RegistrationResponse;
import com.ecommerce.userservice.dto.response.TokenRefreshResponse;
import com.ecommerce.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and session management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest httpRequest) {

        log.info("Registration request received for username: {}", request.getUsername());

        RegistrationResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and sets access/refresh tokens in HttpOnly cookies")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Login request received for username: {}", request.getUsername());

        LoginResponse response = authService.login(request, httpRequest, httpResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
            description = "Uses refresh token from cookie to generate new access token with rotation")
    public ResponseEntity<TokenRefreshResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("Token refresh request received");

        TokenRefreshResponse response = authService.refresh(httpRequest, httpResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes tokens and clears authentication cookies")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Logout request received");
        authService.logout(httpRequest, httpResponse);

        return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Validates the current access token from cookie")
    public ResponseEntity<Map<String, Object>> validateToken() {
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "message", "Token is valid",
                "timestamp", Instant.now().toString()
        ));
    }
}