package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByEnabledTrue();

    List<User> findByLockedTrue();

    boolean existsByUserIdAndEnabledTrueAndLockedFalse(UUID userId);

    @Query("SELECT u FROM User u WHERE (u.username = :identifier OR u.email = :identifier) AND u.enabled = true")
    Optional<User> findByUsernameOrEmailAndEnabledTrue(@Param("identifier") String identifier);
}

