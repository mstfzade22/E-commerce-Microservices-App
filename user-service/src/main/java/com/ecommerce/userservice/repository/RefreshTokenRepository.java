package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findAllBySessionId(String sessionId);

    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.tokenHash = :tokenHash 
          AND rt.userId = :userId 
          AND rt.revokedAt IS NULL 
          AND rt.expiresAt > :now
    """)
    Optional<RefreshToken> findActiveByTokenHashAndUser(
            @Param("tokenHash") String tokenHash,
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.userId = :userId
          AND rt.revokedAt IS NULL
          AND rt.expiresAt > :now
        ORDER BY rt.createdAt ASC
    """)
    List<RefreshToken> findActiveTokensByUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Transactional
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :revokedAt
        WHERE rt.expiresAt < :now
          AND rt.revokedAt IS NULL
    """)
    int revokeExpiredTokens(
            @Param("now") Instant now,
            @Param("revokedAt") Instant revokedAt
    );

    @Transactional
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :revokedAt
        WHERE rt.userId = :userId
          AND rt.revokedAt IS NULL
    """)
    int revokeAllUserTokens(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :threshold")
    int deleteExpiredTokens(@Param("threshold") Instant threshold);

    boolean existsByJti(String jti);

    long countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.jti = :jti")
    Optional<RefreshToken> findByJtiWithLock(@Param("jti") String jti);

    void deleteBySessionId(String sessionId);
}
