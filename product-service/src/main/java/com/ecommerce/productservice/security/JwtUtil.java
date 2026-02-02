package com.ecommerce.productservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Slf4j
public class JwtUtil {

    private final SecretKey accessTokenKey;

    @Value("${jwt.issuer:user-service}")
    private String expectedIssuer;

    public JwtUtil(@Value("${jwt.access.secret}") String accessSecret) {
        if (accessSecret == null || accessSecret.length() < 32) {
            throw new IllegalArgumentException("Access token secret must be at least 32 characters (256 bits)");
        }

        this.accessTokenKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Utility initialized for token validation");
    }

    public Claims validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(accessTokenKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Access token validated successfully, JTI: {}, User: {}, SessionId: {}",
                    claims.getId(), claims.getSubject(), claims.get("sid"));

            return claims;

        } catch (ExpiredJwtException e) {
            log.debug("Access token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported JWT token");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new JwtException("Malformed JWT token");
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new JwtException("JWT claims string is empty");
        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
            throw e;
        }
    }

    public String extractJti(Claims claims) {
        String jti = claims.getId();
        if (jti == null) {
            log.error("JTI claim missing from token");
            throw new JwtException("JTI claim missing from token");
        }
        return jti;
    }

    public String extractSessionId(Claims claims) {
        String sessionId = claims.get("sid", String.class);
        if (sessionId == null) {
            log.error("SessionId claim missing from token");
            throw new JwtException("SessionId claim missing from token");
        }
        return sessionId;
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }


    public Instant getTokenExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }


    public UUID extractUserId(Claims claims) {
        try {
            String subject = claims.getSubject();
            if (subject == null) {
                log.error("Subject claim missing from token");
                throw new JwtException("Subject claim missing from token");
            }
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID in token subject: {}", claims.getSubject());
            throw new JwtException("Invalid user ID in token");
        }
    }

    public long getRemainingTTL(Claims claims) {
        Instant expiration = getTokenExpiration(claims);
        long remaining = ChronoUnit.SECONDS.between(Instant.now(), expiration);
        return Math.max(0, remaining);
    }
}
