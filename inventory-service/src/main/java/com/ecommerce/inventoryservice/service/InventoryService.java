package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.config.RedisConfig;
import com.ecommerce.inventoryservice.dto.event.ProductCreatedEvent;
import com.ecommerce.inventoryservice.dto.request.StockUpdateRequest;
import com.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.StockStatus;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.kafka.InventoryEventProducer;
import com.ecommerce.inventoryservice.mapper.InventoryMapper;
import com.ecommerce.inventoryservice.repository.InventoryRepository;

import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryEventProducer eventProducer;

    @Transactional
    public void createInventory(ProductCreatedEvent event) {
        log.info("Creating inventory for product: {}", event.id());
        Inventory inventory = Inventory.builder()
                .productId(event.id())
                .quantity(event.initialStock() != null ? event.initialStock() : 0)
                .reservedQuantity(0)
                .lowStockThreshold(10)
                .build();

        inventoryRepository.save(inventory);
        eventProducer.sendStockUpdatedEvent(inventory);
    }

    @Cacheable(value = RedisConfig.CacheNames.INVENTORY, key = "#productId")
    public InventoryResponse getStockInfo(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(inventoryMapper::toResponse)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
    }

    @Transactional
    @CachePut(value = RedisConfig.CacheNames.INVENTORY, key = "#productId")
    public InventoryResponse updateStock(Long productId, StockUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));

        if (request.quantity() != null) inventory.setQuantity(request.quantity());
        if (request.lowStockThreshold() != null) inventory.setLowStockThreshold(request.lowStockThreshold());

        Inventory saved = inventoryRepository.save(inventory);
        eventProducer.sendStockUpdatedEvent(saved);

        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.INVENTORY, key = "#productId")
    public void deleteInventory(Long productId) {
        inventoryRepository.findByProductId(productId)
                .ifPresent(inventoryRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockProducts() {
        log.info("Fetching low stock and out of stock products");
        return Stream.concat(
                inventoryRepository.findAllByStockStatus(StockStatus.LOW_STOCK).stream(),
                inventoryRepository.findAllByStockStatus(StockStatus.OUT_OF_STOCK).stream()
        ).map(inventoryMapper::toResponse).toList();
    }
}