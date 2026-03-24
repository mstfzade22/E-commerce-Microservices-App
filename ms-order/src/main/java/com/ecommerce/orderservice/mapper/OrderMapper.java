package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.response.OrderItemResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.orderservice.dto.response.OrderSummaryResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "shippingAddress.addressLine1", source = "shippingAddressLine1")
    @Mapping(target = "shippingAddress.addressLine2", source = "shippingAddressLine2")
    @Mapping(target = "shippingAddress.city", source = "shippingCity")
    @Mapping(target = "shippingAddress.state", source = "shippingState")
    @Mapping(target = "shippingAddress.postalCode", source = "shippingPostalCode")
    @Mapping(target = "shippingAddress.country", source = "shippingCountry")
    OrderResponse toOrderResponse(Order order);

    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    @Mapping(target = "itemCount", expression = "java(order.getItems().size())")
    OrderSummaryResponse toOrderSummaryResponse(Order order);

    OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistory history);
}