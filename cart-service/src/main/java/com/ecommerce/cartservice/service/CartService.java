package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.config.RedisConfig;
import com.ecommerce.cartservice.dto.event.*;
import com.ecommerce.cartservice.dto.request.AddToCartRequest;
import com.ecommerce.cartservice.dto.request.UpdateCartItemRequest;
import com.ecommerce.cartservice.dto.response.*;
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
import com.ecommerce.productservice.grpc.ProductImageInfo;
import com.ecommerce.productservice.grpc.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ProductGrpcClient productGrpcClient;
    private final InventoryGrpcClient inventoryGrpcClient;
    private final CartEventProducer eventProducer;

    @Cacheable(value = RedisConfig.CacheNames.CART, key = "#userId")
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        log.info("Fetching cart for user: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toResponse(cart);
    }

    @CachePut(value = RedisConfig.CacheNames.CART, key = "#userId")
    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        log.info("Adding product {} to cart for user {}", request.productId(), userId);

        ProductResponse product = productGrpcClient.getProduct(request.productId());

        CheckStockResponse stock = inventoryGrpcClient.checkStock(request.productId(), request.quantity());
        if (!stock.getIsAvailable()) {
            throw new InsufficientStockException(request.productId(), request.quantity(), stock.getAvailableQuantity());
        }

        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId())
                .map(existingItem -> {
                    int newTotal = existingItem.getQuantity() + request.quantity();
                    CheckStockResponse totalStock = inventoryGrpcClient.checkStock(request.productId(), newTotal);
                    if (!totalStock.getIsAvailable()) {
                        throw new InsufficientStockException(request.productId(), newTotal, totalStock.getAvailableQuantity());
                    }
                    existingItem.setQuantity(newTotal);
                    return existingItem;
                })
                .orElseGet(() -> {
                    CartItem newItem = CartItem.builder()
                            .cart(cart)
                            .productId(product.getId())
                            .productName(product.getName())
                            .productImageUrl(extractPrimaryImageUrl(product))
                            .unitPrice(new BigDecimal(product.getPrice()))
                            .quantity(request.quantity())
                            .build();
                    cart.addItem(newItem);
                    return newItem;
                });

        cartRepository.save(cart);

        eventProducer.sendItemAddedEvent(new CartItemAddedEvent(
                UUID.randomUUID().toString(), "CART_ITEM_ADDED", userId, cart.getId(),
                item.getProductId(), item.getProductName(), request.quantity(),
                item.getUnitPrice(), Instant.now()
        ));

        return cartMapper.toResponse(cart);
    }

    @CachePut(value = RedisConfig.CacheNames.CART, key = "#userId")
    @Transactional
    public CartResponse updateItemQuantity(UUID userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId, cart.getId()));

        int oldQuantity = item.getQuantity();

        CheckStockResponse stock = inventoryGrpcClient.checkStock(productId, request.quantity());
        if (!stock.getIsAvailable()) {
            throw new InsufficientStockException(productId, request.quantity(), stock.getAvailableQuantity());
        }

        item.setQuantity(request.quantity());
        cartRepository.save(cart);

        eventProducer.sendItemUpdatedEvent(new CartItemUpdatedEvent(
                UUID.randomUUID().toString(), "CART_ITEM_UPDATED", userId, cart.getId(),
                productId, oldQuantity, request.quantity(), Instant.now()
        ));

        return cartMapper.toResponse(cart);
    }

    @CachePut(value = RedisConfig.CacheNames.CART, key = "#userId")
    @Transactional
    public CartResponse removeItem(UUID userId, Long productId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new CartItemNotFoundException(productId, cart.getId());
        }

        cartRepository.save(cart);

        eventProducer.sendItemRemovedEvent(new CartItemRemovedEvent(
                UUID.randomUUID().toString(), "CART_ITEM_REMOVED", userId, cart.getId(),
                productId, Instant.now()
        ));

        return cartMapper.toResponse(cart);
    }

    @CacheEvict(value = RedisConfig.CacheNames.CART, key = "#userId")
    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        int count = cart.getItems().size();
        cart.getItems().clear();
        cartRepository.save(cart);

        eventProducer.sendCartClearedEvent(new CartClearedEvent(
                UUID.randomUUID().toString(), "CART_CLEARED", userId, cart.getId(),
                count, Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public CartSummaryResponse getSummary(UUID userId) {
        log.info("Fetching cart summary for user: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toSummaryResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartValidationResponse validateCart(UUID userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(userId));

        List<Long> productIds = cart.getItems().stream().map(CartItem::getProductId).toList();
        List<ProductResponse> products = productGrpcClient.getProductsByIds(productIds);

        List<CartItemValidation> validations = new ArrayList<>();
        boolean overallValid = true;

        for (CartItem item : cart.getItems()) {
            ProductResponse pResp = products.stream()
                    .filter(p -> p.getId() == item.getProductId())
                    .findFirst().orElse(null);

            boolean available = pResp != null && pResp.getIsActive();
            BigDecimal currentPrice = available ? new BigDecimal(pResp.getPrice()) : BigDecimal.ZERO;
            boolean priceChanged = available && currentPrice.compareTo(item.getUnitPrice()) != 0;

            CheckStockResponse stock = inventoryGrpcClient.checkStock(item.getProductId(), item.getQuantity());

            validations.add(new CartItemValidation(
                    item.getProductId(), item.getProductName(), available,
                    stock.getIsAvailable(), priceChanged, currentPrice,
                    item.getUnitPrice(), item.getQuantity(), stock.getAvailableQuantity()
            ));

            if (!available || !stock.getIsAvailable()) overallValid = false;
        }

        return new CartValidationResponse(overallValid, validations);
    }

    // --- Kafka Event Handlers ---

    @Transactional
    public void handlePriceChange(PriceChangedEvent event) {
        log.info("Handling price change for product {}: {} -> {}", event.id(), event.oldPrice(), event.newPrice());
        List<CartItem> items = cartItemRepository.findByProductId(event.id());
        BigDecimal effectivePrice = event.newDiscountPrice() != null ? event.newDiscountPrice() : event.newPrice();
        items.forEach(item -> {
            item.setUnitPrice(effectivePrice);
            evictUserCache(item.getCart().getUserId());
        });
        cartItemRepository.saveAll(items);
    }

    @Transactional
    public void handleProductDeletion(Long productId) {
        log.info("Handling product deletion for product {}", productId);
        cartItemRepository.deleteByProductId(productId);
    }

    @Transactional
    public void handleStockUpdate(StockUpdatedEvent event) {
        if ("OUT_OF_STOCK".equals(event.stockStatus())) {
            log.warn("Product {} is now OUT_OF_STOCK. Affected cart items should be flagged.", event.productId());
        } else {
            log.info("Stock updated for product {}: quantity={}, status={}",
                    event.productId(), event.newQuantity(), event.stockStatus());
        }
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId)
                        .status(CartStatus.ACTIVE)
                        .items(new ArrayList<>())
                        .build()));
    }

    private String extractPrimaryImageUrl(ProductResponse product) {
        return product.getImagesList().stream()
                .filter(ProductImageInfo::getIsPrimary)
                .findFirst()
                .map(ProductImageInfo::getImageUrl)
                .orElse(product.getImagesList().isEmpty() ? null : product.getImagesList().get(0).getImageUrl());
    }

    @CacheEvict(value = RedisConfig.CacheNames.CART, key = "#userId")
    public void evictUserCache(UUID userId) {
        log.debug("Evicting cache for user {}", userId);
    }
}
