package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.event.PriceChangedEvent;
import com.ecommerce.cartservice.dto.event.StockUpdatedEvent;
import com.ecommerce.cartservice.dto.request.AddToCartRequest;
import com.ecommerce.cartservice.dto.request.UpdateCartItemRequest;
import com.ecommerce.cartservice.dto.response.CartResponse;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.entity.CartStatus;
import com.ecommerce.cartservice.exception.CartItemNotFoundException;
import com.ecommerce.cartservice.exception.CartNotFoundException;
import com.ecommerce.cartservice.exception.InsufficientStockException;
import com.ecommerce.cartservice.grpc.InventoryGrpcClient;
import com.ecommerce.cartservice.grpc.ProductGrpcClient;
import com.ecommerce.cartservice.kafka.CartEventProducer;
import com.ecommerce.cartservice.mapper.CartMapper;
import com.ecommerce.cartservice.repository.CartItemRepository;
import com.ecommerce.cartservice.repository.CartRepository;
import com.ecommerce.inventoryservice.grpc.CheckStockResponse;
import com.ecommerce.productservice.grpc.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private ProductGrpcClient productGrpcClient;
    @Mock
    private InventoryGrpcClient inventoryGrpcClient;
    @Mock
    private CartEventProducer eventProducer;

    @InjectMocks
    private CartService cartService;

    private final UUID userId = UUID.randomUUID();

    private Cart activeCart() {
        return Cart.builder().id(1L).userId(userId).status(CartStatus.ACTIVE).items(new ArrayList<>()).build();
    }

    private CartItem cartItem(Cart cart, Long productId) {
        return CartItem.builder().id(1L).cart(cart).productId(productId)
                .productName("Product").unitPrice(BigDecimal.TEN).quantity(1).build();
    }

    @Test
    void addToCart_newItem_addsSuccessfully() {
        AddToCartRequest request = new AddToCartRequest(1L, 2);
        Cart cart = activeCart();

        ProductResponse product = ProductResponse.newBuilder()
                .setId(1L).setName("Product").setPrice("10.00").setIsActive(true).build();
        CheckStockResponse stock = CheckStockResponse.newBuilder().setIsAvailable(true).setAvailableQuantity(100).build();

        when(productGrpcClient.getProduct(1L)).thenReturn(product);
        when(inventoryGrpcClient.checkStock(1L, 2)).thenReturn(stock);
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse expected = mock(CartResponse.class);
        when(cartMapper.toResponse(cart)).thenReturn(expected);

        CartResponse result = cartService.addToCart(userId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(cart.getItems()).hasSize(1);
        verify(eventProducer).sendItemAddedEvent(any());
    }

    @Test
    void addToCart_insufficientStock_throwsException() {
        AddToCartRequest request = new AddToCartRequest(1L, 100);

        ProductResponse product = ProductResponse.newBuilder()
                .setId(1L).setName("Product").setPrice("10.00").build();
        CheckStockResponse stock = CheckStockResponse.newBuilder().setIsAvailable(false).setAvailableQuantity(5).build();

        when(productGrpcClient.getProduct(1L)).thenReturn(product);
        when(inventoryGrpcClient.checkStock(1L, 100)).thenReturn(stock);

        assertThatThrownBy(() -> cartService.addToCart(userId, request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void updateItemQuantity_updatesSuccessfully() {
        Cart cart = activeCart();
        CartItem item = cartItem(cart, 1L);
        cart.getItems().add(item);
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        CheckStockResponse stock = CheckStockResponse.newBuilder().setIsAvailable(true).setAvailableQuantity(100).build();

        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(item));
        when(inventoryGrpcClient.checkStock(1L, 5)).thenReturn(stock);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse expected = mock(CartResponse.class);
        when(cartMapper.toResponse(cart)).thenReturn(expected);

        CartResponse result = cartService.updateItemQuantity(userId, 1L, request);

        assertThat(result).isEqualTo(expected);
        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void removeItem_existingItem_removesSuccessfully() {
        Cart cart = activeCart();
        CartItem item = cartItem(cart, 1L);
        cart.getItems().add(item);

        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse expected = mock(CartResponse.class);
        when(cartMapper.toResponse(cart)).thenReturn(expected);

        CartResponse result = cartService.removeItem(userId, 1L);

        assertThat(result).isEqualTo(expected);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removeItem_notFound_throwsCartItemNotFoundException() {
        Cart cart = activeCart();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(userId, 999L))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void clearCart_clearsAllItems() {
        Cart cart = activeCart();
        cart.getItems().add(cartItem(cart, 1L));
        cart.getItems().add(cartItem(cart, 2L));

        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(userId);

        assertThat(cart.getItems()).isEmpty();
        verify(eventProducer).sendCartClearedEvent(any());
    }

    @Test
    void clearCart_noCart_throwsCartNotFoundException() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.clearCart(userId))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void handlePriceChange_updatesItemPrice() {
        Cart cart = activeCart();
        CartItem item = cartItem(cart, 1L);
        item.setUnitPrice(BigDecimal.TEN);

        PriceChangedEvent event = new PriceChangedEvent(
                1L, "slug", BigDecimal.TEN, BigDecimal.valueOf(20),
                null, null, Instant.now(), "evt-1", Instant.now());

        when(cartItemRepository.findByProductId(1L)).thenReturn(List.of(item));

        cartService.handlePriceChange(event);

        assertThat(item.getUnitPrice()).isEqualTo(BigDecimal.valueOf(20));
        verify(cartItemRepository).saveAll(List.of(item));
    }

    @Test
    void handlePriceChange_withDiscountPrice_usesDiscountPrice() {
        Cart cart = activeCart();
        CartItem item = cartItem(cart, 1L);

        PriceChangedEvent event = new PriceChangedEvent(
                1L, "slug", BigDecimal.TEN, BigDecimal.valueOf(20),
                null, BigDecimal.valueOf(15), Instant.now(), "evt-1", Instant.now());

        when(cartItemRepository.findByProductId(1L)).thenReturn(List.of(item));

        cartService.handlePriceChange(event);

        assertThat(item.getUnitPrice()).isEqualTo(BigDecimal.valueOf(15));
    }

    @Test
    void handleProductDeletion_deletesCartItems() {
        cartService.handleProductDeletion(1L);

        verify(cartItemRepository).deleteByProductId(1L);
    }

    @Test
    void handleStockUpdate_outOfStock_logsWarning() {
        StockUpdatedEvent event = new StockUpdatedEvent(
                "evt-1", "STOCK_UPDATED", 1L, 0, "OUT_OF_STOCK", Instant.now());

        cartService.handleStockUpdate(event);

        verifyNoInteractions(cartItemRepository);
    }
}
