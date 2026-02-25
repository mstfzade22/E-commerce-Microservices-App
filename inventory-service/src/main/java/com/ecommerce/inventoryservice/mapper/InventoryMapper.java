package com.ecommerce.inventoryservice.mapper;

import com.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.ecommerce.inventoryservice.dto.response.StockStatusResponse;
import com.ecommerce.inventoryservice.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InventoryMapper {

    /**
     * Maps Inventory entity to InventoryResponse DTO
     */
    InventoryResponse toResponse(Inventory inventory);

    /**
     * Maps Inventory entity to StockStatusResponse DTO
     */
    @Mapping(target = "availableQuantity", expression = "java(inventory.getQuantity() - inventory.getReservedQuantity())")
    StockStatusResponse toStatusResponse(Inventory inventory);
}