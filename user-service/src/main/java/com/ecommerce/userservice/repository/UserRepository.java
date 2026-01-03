package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE (u.username = :identifier OR u.email = :identifier) AND u.enabled = true")
    Optional<User> findByUsernameOrEmailAndEnabledTrue(@Param("identifier") String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.userId = :userId AND u.enabled = true AND u.locked = false")
    boolean isUserActiveAndUnlocked(@Param("userId")UUID userId);

    @Query("SELECT u FROM User u WHERE u.enabled = true")
    java.util.List<User> findAllEnabledUsers();

    @Query("SELECT u FROM User u WHERE u.locked = true")
    java.util.List<User> findAllLockedUsers();
}
