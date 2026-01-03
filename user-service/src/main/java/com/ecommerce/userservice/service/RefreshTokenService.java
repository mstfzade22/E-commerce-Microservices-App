package com.ecommerce.userservice.service;

import com.ecommerce.userservice.entity.RefreshToken;
import com.ecommerce.userservice.exception.TokenRevocationException;
import com.ecommerce.userservice.repository.RefreshTokenRepository;
import com.ecommerce.userservice.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;

    @Value("${jwt.refresh.expiration:604800}")
    private long refreshTokenExpiration;

    @Transactional
    public void revokeToken(String jti) {
        log.debug("Revoking refresh token: {}", jti);

        RefreshToken token = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> {
                    log.warn("Cannot revoke - refresh token not found: {}", jti);
                    return new TokenRevocationException("Refresh token not found: " + jti);
                });

        if (token.getRevokedAt() != null) {
            log.debug("Token already revoked: {}", jti);
            return;
        }

        token.revoke();
        refreshTokenRepository.save(token);

        log.info("Refresh token revoked: {}", jti);
    }

    @Transactional
    public void revokeAllUserTokensByUserId(UUID userId) {
        log.warn("Revoking ALL refresh tokens for user: {}", userId);

        Instant now = Instant.now();
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(userId, now);

        log.info("SECURITY EVENT - Revoked {} refresh tokens for user: {} due to security event",
                revokedCount, userId);
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired refresh tokens");

        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteExpiredTokens(threshold);

        log.info("Cleaned up {} expired refresh tokens older than 30 days", deleted);
    }

    @Transactional
    @Scheduled(cron = "0 30 2 * * ?")  // Every day at 2:30 AM
    public void cleanupExpiredSessions() {
        log.info("Starting scheduled cleanup of expired sessions");

        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = sessionRepository.deleteExpiredSessions(threshold);

        log.info("Cleaned up {} expired sessions older than 30 days", deleted);
    }
}