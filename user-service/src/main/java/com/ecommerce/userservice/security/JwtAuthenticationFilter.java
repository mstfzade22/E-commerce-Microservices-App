package com.ecommerce.userservice.security;

import com.ecommerce.userservice.service.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        if (isPublicEndpoint(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = extractToken(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(jwt, request);
            }
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired for request: {} - {}", requestUri, e.getMessage());
            handleAuthenticationError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token expired", "Please refresh your token");
            return;
        } catch (JwtException e) {
            log.warn("JWT validation failed for request: {} - {}", requestUri, e.getMessage());
            handleAuthenticationError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid token", "Authentication failed");
            return;
        } catch (Exception e) {
            log.error("Authentication error for request: {}", requestUri, e);
            handleAuthenticationError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Authentication error", "An error occurred during authentication");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(String jwt, HttpServletRequest request) {
        Claims claims = jwtUtil.validateAccessToken(jwt);

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        if (userId == null || role == null) {
            throw new JwtException("Invalid token claims");
        }

        UserDetails userDetails = User.builder()
                .username(userId)
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)))
                .build();

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("User (ID: {}) authenticated successfully with role: {}", userId, role);
    }

    private String extractToken(HttpServletRequest request) {
        // First try Authorization header (for Swagger/API clients)
        String bearerToken = extractTokenFromHeader(request);
        if (bearerToken != null) {
            return bearerToken;
        }
        // Fall back to cookie (for browser clients)
        return extractTokenFromCookie(request);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isPublicEndpoint(String requestUri) {
        return requestUri.contains("/auth/register") ||
                requestUri.contains("/auth/login") ||
                requestUri.contains("/auth/refresh") ||
                requestUri.contains("/actuator/health") ||
                requestUri.contains("/actuator/info") ||
                requestUri.contains("/v3/api-docs") ||
                requestUri.contains("/swagger-ui") ||
                requestUri.equals("/swagger-ui.html");
    }

    private void handleAuthenticationError(HttpServletResponse response, int status,
                                           String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                java.time.Instant.now().toString(), status, error, message
        ));
    }
}