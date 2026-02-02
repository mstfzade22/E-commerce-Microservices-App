package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegistrationRequest;
import com.ecommerce.userservice.dto.response.LoginResponse;
import com.ecommerce.userservice.dto.response.RegistrationResponse;
import com.ecommerce.userservice.dto.response.TokenRefreshResponse;
import com.ecommerce.userservice.service.AuthService;
import com.ecommerce.userservice.service.AuthService.AuthResult;
import com.ecommerce.userservice.service.AuthService.TokenPair;
import com.ecommerce.userservice.service.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
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
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request) {

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

        AuthResult<LoginResponse> result = authService.login(
                request,
                getClientIp(httpRequest),
                getUserAgent(httpRequest)
        );

        setAuthCookies(httpResponse, result.getTokens());

        return ResponseEntity.ok(result.getResponse());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
            description = "Uses refresh token from cookie to generate new access token with rotation")
    public ResponseEntity<TokenRefreshResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("Token refresh request received");

        String refreshToken = extractTokenFromCookie(httpRequest, "refresh_token");

        AuthResult<TokenRefreshResponse> result = authService.refresh(
                refreshToken,
                getClientIp(httpRequest),
                getUserAgent(httpRequest)
        );

        setAuthCookies(httpResponse, result.getTokens());

        return ResponseEntity.ok(result.getResponse());
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes tokens and clears authentication cookies")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Logout request received");

        String refreshToken = extractTokenFromCookie(httpRequest, "refresh_token");
        authService.logout(refreshToken);
        clearAuthCookies(httpResponse);

        return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Validates the current access token and returns user info")
    public ResponseEntity<Map<String, Object>> validateToken(
            HttpServletRequest httpRequest) {

        String accessToken = extractTokenFromCookie(httpRequest, "access_token");
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "No access token found"));
        }

        try {
            var claims = jwtUtil.validateAccessToken(accessToken);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", claims.getSubject(),
                    "role", claims.get("role", String.class),
                    "sessionId", claims.get("sid", String.class),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", e.getMessage()));
        }
    }

    private void setAuthCookies(HttpServletResponse response, TokenPair tokens) {
        Cookie accessCookie = new Cookie("access_token", tokens.accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) jwtUtil.getAccessTokenExpiration());
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", tokens.refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) jwtUtil.getRefreshTokenExpiration());
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setMaxAge(0);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setMaxAge(0);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);
    }

    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }
}

