package com.ecommerce.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/",
            "/api/v1/payments/callback/",
            "/actuator/"
    );

    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/v1/products/",
            "/api/v1/products",
            "/api/v1/categories/",
            "/api/v1/categories",
            "/api/v1/inventory/"
    );

    @Value("${jwt.access.secret}")
    private String secret;

    private final ReactiveStringRedisTemplate redisTemplate;

    private SecretKey key;

    public JwtAuthGatewayFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Public paths — pass through without auth, strip any internal headers to prevent spoofing
        if (isPublicPath(path, method)) {
            ServerHttpRequest cleaned = request.mutate()
                    .headers(h -> {
                        h.remove("X-User-Id");
                        h.remove("X-User-Role");
                    })
                    .build();
            return chain.filter(exchange.mutate().request(cleaned).build());
        }

        // Protected paths — validate JWT
        String token = extractToken(request);

        if (token == null) {
            return onError(exchange.getResponse(), "Missing authorization token", HttpStatus.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return onError(exchange.getResponse(), "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        String jti = claims.get("jti", String.class);

        if (userId == null || role == null) {
            return onError(exchange.getResponse(), "Invalid token claims", HttpStatus.UNAUTHORIZED);
        }

        // Check Redis denylist for revoked tokens
        String denylistKey = "jwt:denylist:" + jti;
        return redisTemplate.hasKey(denylistKey)
                .flatMap(isDenied -> {
                    if (Boolean.TRUE.equals(isDenied)) {
                        log.warn("Token {} has been revoked", jti);
                        return onError(exchange.getResponse(), "Token has been revoked", HttpStatus.UNAUTHORIZED);
                    }

                    // Strip Authorization header and inject internal headers
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role)
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                });
    }

    private boolean isPublicPath(String path, String method) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }

        if ("GET".equals(method)) {
            for (String prefix : PUBLIC_GET_PREFIXES) {
                if (path.startsWith(prefix) || path.equals(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        HttpCookie cookie = request.getCookies().getFirst("access_token");
        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }

    private Mono<Void> onError(ServerHttpResponse response, String message, HttpStatus status) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
