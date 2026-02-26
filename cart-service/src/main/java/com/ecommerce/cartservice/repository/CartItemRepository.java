package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Finds a specific product within a specific cart.
     * Used to determine if we should increment quantity or add a new row.
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Finds all cart items across all users for a specific product.
     * Essential for the PriceChangedEvent consumer to update snapshots.
     */
    List<CartItem> findByProductId(Long productId);

    /**
     * Removes all items for a product from all carts.
     * Used when a ProductDeletedEvent is received from Kafka.
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    /**
     * Clears all items from a specific cart.
     * Used by the "Clear Cart" REST endpoint.
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}