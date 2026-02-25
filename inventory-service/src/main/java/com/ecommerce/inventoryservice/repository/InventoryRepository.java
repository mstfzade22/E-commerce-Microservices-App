package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Crucial for sync with Product Service and gRPC stock checks
    Optional<Inventory> findByProductId(Long productId);

    // Used for the admin "low stock" dashboard requirement
    java.util.List<Inventory> findAllByStockStatus(com.ecommerce.inventoryservice.entity.StockStatus status);
}