package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.event.ProductCreatedEvent;
import com.ecommerce.inventoryservice.dto.request.StockUpdateRequest;
import com.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.StockStatus;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.kafka.InventoryEventProducer;
import com.ecommerce.inventoryservice.mapper.InventoryMapper;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private InventoryEventProducer eventProducer;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void createInventory_savesAndPublishesEvent() {
        ProductCreatedEvent event = new ProductCreatedEvent(
                1L, "Product", "product", "SKU-001", BigDecimal.TEN,
                null, null, true, 100, Instant.now(), "evt-1", Instant.now());

        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.createInventory(event);

        verify(inventoryRepository).save(any(Inventory.class));
        verify(eventProducer).sendStockUpdatedEvent(any(Inventory.class));
    }

    @Test
    void createInventory_nullInitialStock_defaultsToZero() {
        ProductCreatedEvent event = new ProductCreatedEvent(
                1L, "Product", "product", "SKU-001", BigDecimal.TEN,
                null, null, true, null, Instant.now(), "evt-1", Instant.now());

        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            assertThat(i.getQuantity()).isEqualTo(0);
            return i;
        });

        inventoryService.createInventory(event);

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void getStockInfo_found_returnsResponse() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(50)
                .reservedQuantity(0).lowStockThreshold(10).stockStatus(StockStatus.AVAILABLE).build();
        InventoryResponse expected = new InventoryResponse(1L, 50, 0, 10, StockStatus.AVAILABLE, null, null);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryMapper.toResponse(inventory)).thenReturn(expected);

        InventoryResponse result = inventoryService.getStockInfo(1L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getStockInfo_notFound_throwsInventoryNotFoundException() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStockInfo(999L))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void updateStock_updatesQuantityAndPublishesEvent() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(50)
                .reservedQuantity(0).lowStockThreshold(10).stockStatus(StockStatus.AVAILABLE).build();
        StockUpdateRequest request = new StockUpdateRequest(100, 20);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse expected = new InventoryResponse(1L, 100, 0, 20, StockStatus.AVAILABLE, null, null);
        when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(expected);

        InventoryResponse result = inventoryService.updateStock(1L, request);

        assertThat(result.quantity()).isEqualTo(100);
        assertThat(inventory.getQuantity()).isEqualTo(100);
        assertThat(inventory.getLowStockThreshold()).isEqualTo(20);
        verify(eventProducer).sendStockUpdatedEvent(any(Inventory.class));
    }

    @Test
    void updateStock_notFound_throwsInventoryNotFoundException() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateStock(999L, new StockUpdateRequest(10, null)))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void updateOrCreateStock_existingInventory_updates() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(50)
                .reservedQuantity(0).lowStockThreshold(10).stockStatus(StockStatus.AVAILABLE).build();

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse expected = new InventoryResponse(1L, 200, 0, 10, StockStatus.AVAILABLE, null, null);
        when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(expected);

        InventoryResponse result = inventoryService.updateOrCreateStock(1L, 200);

        assertThat(result.quantity()).isEqualTo(200);
        assertThat(inventory.getQuantity()).isEqualTo(200);
    }

    @Test
    void updateOrCreateStock_newInventory_creates() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse expected = new InventoryResponse(999L, 30, 0, 10, StockStatus.AVAILABLE, null, null);
        when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(expected);

        InventoryResponse result = inventoryService.updateOrCreateStock(999L, 30);

        assertThat(result).isNotNull();
        verify(inventoryRepository).save(any(Inventory.class));
        verify(eventProducer).sendStockUpdatedEvent(any(Inventory.class));
    }

    @Test
    void deleteInventory_existingProduct_deletes() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).build();
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        inventoryService.deleteInventory(1L);

        verify(inventoryRepository).delete(inventory);
    }

    @Test
    void getLowStockProducts_returnsCombinedList() {
        Inventory low = Inventory.builder().id(1L).productId(1L).quantity(5)
                .stockStatus(StockStatus.LOW_STOCK).build();
        Inventory out = Inventory.builder().id(2L).productId(2L).quantity(0)
                .stockStatus(StockStatus.OUT_OF_STOCK).build();

        when(inventoryRepository.findAllByStockStatus(StockStatus.LOW_STOCK)).thenReturn(List.of(low));
        when(inventoryRepository.findAllByStockStatus(StockStatus.OUT_OF_STOCK)).thenReturn(List.of(out));

        InventoryResponse lowResp = new InventoryResponse(1L, 5, 0, 10, StockStatus.LOW_STOCK, null, null);
        InventoryResponse outResp = new InventoryResponse(2L, 0, 0, 10, StockStatus.OUT_OF_STOCK, null, null);
        when(inventoryMapper.toResponse(low)).thenReturn(lowResp);
        when(inventoryMapper.toResponse(out)).thenReturn(outResp);

        List<InventoryResponse> result = inventoryService.getLowStockProducts();

        assertThat(result).hasSize(2);
    }
}
