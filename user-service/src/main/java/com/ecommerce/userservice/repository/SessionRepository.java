package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findBySessionId(String sessionId);
    List<Session> findByUserIdAndIsActiveTrue(UUID userId);

    @Transactional
    @Modifying
    int deleteByExpiresAtBefore(Instant threshold);

    @Transactional
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt WHERE s.userId = :userId AND s.isActive = true")
    void revokeAllUserSessions(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}