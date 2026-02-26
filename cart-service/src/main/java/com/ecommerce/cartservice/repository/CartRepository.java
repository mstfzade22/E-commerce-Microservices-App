package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Finds the active cart for a specific user.
     * Used during most cart operations (add, update, get).
     */
    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    /**
     * Checks if a user already has a cart with a specific status.
     */
    boolean existsByUserIdAndStatus(UUID userId, CartStatus status);
}