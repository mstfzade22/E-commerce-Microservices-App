package com.ecommerce.orderservice.service;

import com.ecommerce.inventoryservice.grpc.ConfirmStockResponse;
import com.ecommerce.inventoryservice.grpc.ReleaseStockResponse;
import com.ecommerce.inventoryservice.grpc.ReserveStockResponse;
import com.ecommerce.orderservice.client.CartServiceClient;
import com.ecommerce.orderservice.dto.request.CancelOrderRequest;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.response.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.*;
import com.ecommerce.orderservice.grpc.InventoryGrpcClient;
import com.ecommerce.orderservice.grpc.ProductGrpcClient;
import com.ecommerce.orderservice.kafka.OrderEventProducer;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.productservice.grpc.ProductImageInfo;
import com.ecommerce.productservice.grpc.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private CartServiceClient cartServiceClient;
    @Mock
    private ProductGrpcClient productGrpcClient;
    @Mock
    private InventoryGrpcClient inventoryGrpcClient;
    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private final UUID userId = UUID.randomUUID();
    private final String role = "ROLE_CUSTOMER";

    private CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest("123 Main St", null, "New York", "NY", "10001", "US", null);
    }

    private CartItemResponse cartItem(Long productId, int quantity) {
        return new CartItemResponse(1L, productId, "Product", null, quantity, BigDecimal.TEN, BigDecimal.TEN);
    }

    private ProductResponse mockProduct(Long productId) {
        ProductImageInfo image = ProductImageInfo.newBuilder()
                .setImageUrl("http://img.com/1.jpg")
                .setIsPrimary(true)
                .build();
        return ProductResponse.newBuilder()
                .setId(productId)
                .setName("Product " + productId)
                .setSku("SKU-" + productId)
                .setPrice("10.00")
                .setDiscountPrice("")
                .addImages(image)
                .build();
    }

    @Test
    void createOrder_happyPath() {
        CreateOrderRequest request = createOrderRequest();
        List<CartItemResponse> items = List.of(cartItem(1L, 2));
        CartResponse cart = new CartResponse(1L, userId, items, BigDecimal.valueOf(20), "ACTIVE");
        CartValidationResponse validation = new CartValidationResponse(true, null, List.of());

        when(cartServiceClient.getCart(userId, role)).thenReturn(cart);
        when(cartServiceClient.validateCart(userId, role)).thenReturn(validation);
        when(productGrpcClient.getProduct(1L)).thenReturn(mockProduct(1L));

        ReserveStockResponse reserveResponse = ReserveStockResponse.newBuilder().setSuccess(true).build();
        when(inventoryGrpcClient.reserveStock(anyString(), eq(1L), eq(2))).thenReturn(reserveResponse);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            o.setCreatedAt(Instant.now());
            return o;
        });

        OrderCreateResponse result = orderService.createOrder(userId, request, role);

        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).startsWith("ORD-");
        verify(orderRepository).save(any(Order.class));
        verify(orderEventProducer).sendOrderCreatedEvent(any());
    }

    @Test
    void createOrder_emptyCart_throwsCartEmptyException() {
        CartResponse cart = new CartResponse(1L, userId, List.of(), BigDecimal.ZERO, "ACTIVE");
        when(cartServiceClient.getCart(userId, role)).thenReturn(cart);

        assertThatThrownBy(() -> orderService.createOrder(userId, createOrderRequest(), role))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    void createOrder_nullCartItems_throwsCartEmptyException() {
        CartResponse cart = new CartResponse(1L, userId, null, BigDecimal.ZERO, "ACTIVE");
        when(cartServiceClient.getCart(userId, role)).thenReturn(cart);

        assertThatThrownBy(() -> orderService.createOrder(userId, createOrderRequest(), role))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    void createOrder_cartValidationFails_throwsCartValidationException() {
        List<CartItemResponse> items = List.of(cartItem(1L, 1));
        CartResponse cart = new CartResponse(1L, userId, items, BigDecimal.TEN, "ACTIVE");
        CartValidationResponse validation = new CartValidationResponse(false, "Invalid", List.of("Out of stock"));

        when(cartServiceClient.getCart(userId, role)).thenReturn(cart);
        when(cartServiceClient.validateCart(userId, role)).thenReturn(validation);

        assertThatThrownBy(() -> orderService.createOrder(userId, createOrderRequest(), role))
                .isInstanceOf(CartValidationException.class);
    }

    @Test
    void createOrder_stockReservationFails_releasesAlreadyReserved() {
        List<CartItemResponse> items = List.of(cartItem(1L, 1), cartItem(2L, 1));
        CartResponse cart = new CartResponse(1L, userId, items, BigDecimal.valueOf(20), "ACTIVE");
        CartValidationResponse validation = new CartValidationResponse(true, null, List.of());

        when(cartServiceClient.getCart(userId, role)).thenReturn(cart);
        when(cartServiceClient.validateCart(userId, role)).thenReturn(validation);
        when(productGrpcClient.getProduct(1L)).thenReturn(mockProduct(1L));
        when(productGrpcClient.getProduct(2L)).thenReturn(mockProduct(2L));

        ReserveStockResponse success = ReserveStockResponse.newBuilder().setSuccess(true).build();
        ReserveStockResponse fail = ReserveStockResponse.newBuilder().setSuccess(false).build();
        when(inventoryGrpcClient.reserveStock(anyString(), eq(1L), eq(1))).thenReturn(success);
        when(inventoryGrpcClient.reserveStock(anyString(), eq(2L), eq(1))).thenReturn(fail);

        assertThatThrownBy(() -> orderService.createOrder(userId, createOrderRequest(), role))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryGrpcClient).releaseStock(anyString(), eq(1L));
    }

    @Test
    void confirmOrder_validTransition() {
        Order order = Order.builder().orderNumber("ORD-001").userId(userId).status(OrderStatus.PENDING)
                .items(new LinkedHashSet<>()).statusHistory(new LinkedHashSet<>()).build();
        OrderResponse expected = mock(OrderResponse.class);

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(expected);

        OrderResponse result = orderService.confirmOrder("ORD-001", userId);

        assertThat(result).isEqualTo(expected);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirmOrder_invalidTransition_throwsInvalidOrderStatusException() {
        Order order = Order.builder().orderNumber("ORD-001").userId(userId).status(OrderStatus.DELIVERED)
                .items(new LinkedHashSet<>()).statusHistory(new LinkedHashSet<>()).build();

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder("ORD-001", userId))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void handlePaymentSuccess_confirmedOrder_transitionsToProcessing() {
        OrderItem item = OrderItem.builder().productId(1L).quantity(2).build();
        Order order = Order.builder().orderNumber("ORD-001").userId(userId).status(OrderStatus.CONFIRMED)
                .items(new LinkedHashSet<>(Set.of(item))).statusHistory(new LinkedHashSet<>()).build();

        ConfirmStockResponse confirmResponse = ConfirmStockResponse.newBuilder().setSuccess(true).build();

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));
        when(inventoryGrpcClient.confirmStock("ORD-001", 1L)).thenReturn(confirmResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.handlePaymentSuccess("ORD-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderEventProducer).sendOrderConfirmedEvent(any());
    }

    @Test
    void handlePaymentSuccess_alreadyProcessing_idempotent() {
        Order order = Order.builder().orderNumber("ORD-001").userId(userId).status(OrderStatus.PROCESSING)
                .items(new LinkedHashSet<>()).statusHistory(new LinkedHashSet<>()).build();

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        orderService.handlePaymentSuccess("ORD-001");

        verify(inventoryGrpcClient, never()).confirmStock(anyString(), anyLong());
        verify(orderEventProducer, never()).sendOrderConfirmedEvent(any());
    }

    @Test
    void handlePaymentFailure_releasesStockAndCancels() {
        OrderItem item = OrderItem.builder().productId(1L).quantity(2).build();
        Order order = Order.builder().orderNumber("ORD-001").userId(userId).status(OrderStatus.CONFIRMED)
                .items(new LinkedHashSet<>(Set.of(item))).statusHistory(new LinkedHashSet<>()).build();

        ReleaseStockResponse releaseResponse = ReleaseStockResponse.newBuilder().setSuccess(true).build();

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));
        when(inventoryGrpcClient.releaseStock("ORD-001", 1L)).thenReturn(releaseResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.handlePaymentFailure("ORD-001", "Card declined");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledReason()).isEqualTo("Card declined");
        verify(orderEventProducer).sendOrderCancelledEvent(any());
    }

    @Test
    void cancelOrder_customerCanOnlyCancelPending() {
        Order order = Order.builder().orderNumber("ORD-001").userId(userId).status(OrderStatus.CONFIRMED)
                .items(new LinkedHashSet<>()).statusHistory(new LinkedHashSet<>()).build();

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        CancelOrderRequest cancelRequest = new CancelOrderRequest("Changed my mind");

        assertThatThrownBy(() -> orderService.cancelOrder("ORD-001", userId, cancelRequest, "ROLE_CUSTOMER"))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void cancelOrder_adminCanCancelConfirmed() {
        OrderItem item = OrderItem.builder().productId(1L).quantity(1).build();
        Order order = Order.builder().orderNumber("ORD-001").userId(userId).status(OrderStatus.CONFIRMED)
                .items(new LinkedHashSet<>(Set.of(item))).statusHistory(new LinkedHashSet<>()).build();
        OrderResponse expected = mock(OrderResponse.class);

        ReleaseStockResponse releaseResponse = ReleaseStockResponse.newBuilder().setSuccess(true).build();

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));
        when(inventoryGrpcClient.releaseStock("ORD-001", 1L)).thenReturn(releaseResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(expected);

        CancelOrderRequest cancelRequest = new CancelOrderRequest("Admin cancel");
        OrderResponse result = orderService.cancelOrder("ORD-001", userId, cancelRequest, "ROLE_ADMIN");

        assertThat(result).isEqualTo(expected);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
