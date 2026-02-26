package com.ecommerce.cartservice.mapper;

import com.ecommerce.cartservice.dto.response.CartItemResponse;
import com.ecommerce.cartservice.dto.response.CartResponse;
import com.ecommerce.cartservice.dto.response.CartSummaryResponse;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CartMapper {

    @Mapping(source = "id", target = "cartId")
    @Mapping(source = "items", target = "items")
    @Mapping(target = "totalPrice", source = "items", qualifiedByName = "calculateTotalPrice")
    @Mapping(target = "totalItems", source = "items", qualifiedByName = "calculateTotalItems")
    CartResponse toResponse(Cart cart);

    @Mapping(source = "createdAt", target = "addedAt")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    @Mapping(target = "inStock", constant = "true") // Default value; service will update based on gRPC check
    CartItemResponse toItemResponse(CartItem item);

    @Mapping(target = "totalPrice", source = "items", qualifiedByName = "calculateTotalPrice")
    @Mapping(target = "totalItems", source = "items", qualifiedByName = "calculateTotalItems")
    @Mapping(target = "uniqueProducts", expression = "java(cart.getItems().size())")
    CartSummaryResponse toSummaryResponse(Cart cart);

    default BigDecimal calculateSubtotal(CartItem item) {
        if (item == null || item.getUnitPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    @Named("calculateTotalPrice")
    default BigDecimal calculateTotalPrice(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(this::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Named("calculateTotalItems")
    default Integer calculateTotalItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}