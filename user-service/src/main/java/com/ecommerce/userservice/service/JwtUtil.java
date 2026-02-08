package com.ecommerce.userservice.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtUtil {

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;

    @Getter
    private final long accessTokenExpiration;

    @Getter
    private final long refreshTokenExpiration;

    private final String issuer;

    public JwtUtil(@Value("${jwt.access.secret}") String accessSecret,
                   @Value("${jwt.refresh.secret}") String refreshSecret,
                   @Value("${jwt.access.expiration}") long accessTokenExpiration,
                   @Value("${jwt.refresh.expiration}") long refreshTokenExpiration,
                   @Value("${jwt.issuer:user-service}") String issuer) {

        if (accessSecret == null || accessSecret.length() < 32) {
            throw new IllegalArgumentException("Access token secret must be at least 32 characters (256 bits)");
        }
        if (refreshSecret == null || refreshSecret.length() < 32) {
            throw new IllegalArgumentException("Refresh token secret must be at least 32 characters (256 bits)");
        }

        this.accessTokenKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.issuer = issuer;

        log.info("JWT Service initialized with issuer: {}", issuer);
        log.debug("Access token expiration: {} seconds", accessTokenExpiration);
        log.debug("Refresh token expiration: {} seconds", refreshTokenExpiration);
    }


    public record TokenData(String token, String jti) {}


    public TokenData generateAccessToken(UUID userId, String sessionId, String role) {
        String jti = "access_" + UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);

        String token = Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .subject(userId.toString())
                .claim("type", "access")
                .claim("sid", sessionId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(accessTokenKey, Jwts.SIG.HS512)
                .compact();

        log.debug("Generated access token for user: {}, SessionId: {}, JTI: {}, Role: {}, expires: {}",
                userId, sessionId, jti, role, expiry);

        return new TokenData(token, jti);
    }


    public TokenData generateRefreshToken(UUID userId, String sessionId) {
        String jti = "refresh_" + UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS);

        String token = Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .subject(userId.toString())
                .claim("type", "refresh")
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(refreshTokenKey, Jwts.SIG.HS512)
                .compact();

        log.debug("Generated refresh token for user: {}, SessionId: {}, JTI: {}, expires: {}",
                userId, sessionId, jti, expiry);

        return new TokenData(token, jti);
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

    public Claims validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(refreshTokenKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Refresh token validated successfully, JTI: {}, User: {}, SessionId: {}",
                    claims.getId(), claims.getSubject(), claims.get("sid"));

            return claims;

        } catch (ExpiredJwtException e) {
            log.debug("Refresh token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported refresh JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported JWT token");
        } catch (MalformedJwtException e) {
            log.warn("Malformed refresh JWT token: {}", e.getMessage());
            throw new JwtException("Malformed JWT token");
        } catch (SignatureException e) {
            log.warn("Invalid refresh JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            log.warn("Refresh JWT claims string is empty: {}", e.getMessage());
            throw new JwtException("JWT claims string is empty");
        } catch (JwtException e) {
            log.warn("Refresh JWT validation error: {}", e.getMessage());
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
        } catch (NumberFormatException e) {
            log.error("Invalid user ID in token subject: {}", claims.getSubject());
            throw new JwtException("Invalid user ID in token");
        }
    }

    public long getRemainingTTL(Claims claims) {
        Instant expiration = getTokenExpiration(claims);
        long remaining = ChronoUnit.SECONDS.between(Instant.now(), expiration);
        return Math.max(0, remaining);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpiration * 1000;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpiration * 1000;
    }
}