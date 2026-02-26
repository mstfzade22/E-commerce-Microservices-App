# Prompt: Create Cart Service for E-Commerce Microservices App

## PROJECT CONTEXT

I am building an E-Commerce Microservices application using Java 21, Spring Boot 4.0.2, and Gradle 9.3. The project is a pet project — I want modern solutions but NOT excessive complexity.

### Existing Services

**1. User Service** (port 8081, context-path: /api/v1)
- Handles authentication: registration, login, JWT token issuance
- PostgreSQL DB: `user_service_db`
- Redis: session management, token blacklisting
- Vault: stores JWT secrets, DB credentials at `secret/user-service`
- Roles: CUSTOMER, STORE, ADMIN
- JWT: issues access tokens signed with HS512, claims contain: `sub` (userId as UUID), `role`, `sid` (sessionId), `type`, `jti`
- Tokens are sent via `access_token` HTTP cookie OR `Authorization: Bearer <token>` header

**2. Product Service** (port 8082, gRPC port 9090, context-path: /api/v1)
- Manages products, categories, product images
- PostgreSQL DB: `product_service_db`
- Redis: caches products by ID, by slug, categories by ID, by slug, category tree
- Vault: reads JWT secrets from `secret/user-service` for token validation, DB credentials from `secret/product-service`
- Kafka PRODUCER: publishes to `product-events` topic
- Kafka CONSUMER: listens to `inventory-events` topic
- gRPC SERVER: exposes product data to other services (port 9090)
- MinIO: stores product images
- Swagger: OpenAPI at `/v3/api-docs`, UI at `/swagger-ui`

**3. Inventory Service** (port 8083, gRPC port 9091, context-path: /api/v1)
- Manages product stock levels and reservations
- PostgreSQL DB: `inventory_service_db`
- Redis: caches stock info per product (key: `inventory:{productId}`, TTL 5min)
- Vault: reads JWT secrets from `secret/user-service`, DB/Redis credentials from `secret/inventory-service`
- Kafka PRODUCER: publishes to `inventory-events` topic (STOCK_UPDATED, STOCK_RESERVED, STOCK_RELEASED, STOCK_CONFIRMED)
- Kafka CONSUMER: listens to `product-events` topic (ProductCreatedEvent, ProductDeletedEvent)
- gRPC SERVER: exposes stock operations (CheckStock, ReserveStock, ReleaseStock, ConfirmStock, GetStockInfo)

---

## INFRASTRUCTURE (docker-compose.yml)

All infrastructure runs via Docker Compose with these services:
- **PostgreSQL 16** — port 5432 (single instance, multiple databases)
- **Redis 7** — port 6379
- **HashiCorp Vault 1.15** — port 8200 (dev mode, root-token: `root-token`)
- **MinIO** — port 9000 (API), port 9001 (Console)
- **Apache Kafka 3.7.0** (KRaft mode, no ZooKeeper) — port 9092 (internal), port 29092 (host)
- **Redpanda Console** — port 8080 (Kafka UI)
- **Redis Insight** — port 5540 (Redis UI)

---

## EXISTING gRPC SERVICE DEFINITIONS

### Product Service gRPC Server (port 9090) — product.proto

```protobuf
syntax = "proto3";
package com.ecommerce.productservice.grpc;

option java_multiple_files = true;
option java_package = "com.ecommerce.productservice.grpc";

service ProductGrpcService {
    rpc GetProduct(GetProductRequest) returns (ProductResponse);
    rpc GetProductsByIds(GetProductsByIdsRequest) returns (ProductListResponse);
    rpc CheckProductExists(CheckProductExistsRequest) returns (CheckProductExistsResponse);
    rpc GetProductStockStatus(GetProductStockStatusRequest) returns (ProductStockStatusResponse);
    rpc GetProductsByCategory(GetProductsByCategoryRequest) returns (ProductListResponse);
}

message GetProductRequest {
    int64 product_id = 1;
}

message GetProductsByIdsRequest {
    repeated int64 product_ids = 1;
}

message CheckProductExistsRequest {
    int64 product_id = 1;
}

message CheckProductExistsResponse {
    bool exists = 1;
}

message GetProductStockStatusRequest {
    int64 product_id = 1;
}

message ProductStockStatusResponse {
    int64 product_id = 1;
    StockStatus stock_status = 2;
}

message GetProductsByCategoryRequest {
    int64 category_id = 1;
}

enum StockStatus {
    STOCK_STATUS_UNSPECIFIED = 0;
    AVAILABLE = 1;
    LOW_STOCK = 2;
    OUT_OF_STOCK = 3;
}

message ProductResponse {
    int64 id = 1;
    string name = 2;
    string slug = 3;
    string sku = 4;
    double price = 5;
    double discount_price = 6;
    string short_description = 7;
    string primary_image_url = 8;
    int64 category_id = 9;
    string category_name = 10;
    StockStatus stock_status = 11;
    bool is_active = 12;
}

message ProductListResponse {
    repeated ProductResponse products = 1;
}
```

### Inventory Service gRPC Server (port 9091) — inventory.proto

```protobuf
syntax = "proto3";
package com.ecommerce.inventoryservice.grpc;

option java_multiple_files = true;
option java_package = "com.ecommerce.inventoryservice.grpc";

service InventoryGrpcService {
    rpc CheckStock(CheckStockRequest) returns (CheckStockResponse);
    rpc ReserveStock(ReserveStockRequest) returns (ReserveStockResponse);
    rpc ReleaseStock(ReleaseStockRequest) returns (ReleaseStockResponse);
    rpc ConfirmStock(ConfirmStockRequest) returns (ConfirmStockResponse);
    rpc GetStockInfo(GetStockInfoRequest) returns (StockInfoResponse);
}

message CheckStockRequest {
    int64 product_id = 1;
    int32 quantity = 2;
}

message CheckStockResponse {
    bool is_available = 1;
    int32 available_quantity = 2;
}

message ReserveStockRequest {
    string order_id = 1;
    int64 product_id = 2;
    int32 quantity = 3;
}

message ReserveStockResponse {
    bool success = 1;
    string message = 2;
}

message ReleaseStockRequest {
    string order_id = 1;
    int64 product_id = 2;
}

message ReleaseStockResponse {
    bool success = 1;
    string message = 2;
}

message ConfirmStockRequest {
    string order_id = 1;
    int64 product_id = 2;
}

message ConfirmStockResponse {
    bool success = 1;
    string message = 2;
}

message GetStockInfoRequest {
    int64 product_id = 1;
}

message StockInfoResponse {
    int64 product_id = 1;
    int32 quantity = 2;
    int32 reserved_quantity = 3;
    int32 available_quantity = 4;
    string stock_status = 5;
}
```

---

## EXISTING KAFKA TOPICS & EVENT STRUCTURES

### Topic: `product-events` (produced by Product Service)

**ProductCreatedEvent:**
```json
{
    "id": 1,
    "name": "iPhone 15",
    "slug": "iphone-15",
    "sku": "IP15-001",
    "price": 999.00,
    "categoryId": 1,
    "categoryName": "Smartphones",
    "isActive": true,
    "initialStock": 100,
    "createdAt": "2026-02-20T10:00:00Z",
    "eventId": "uuid-string",
    "timestamp": "2026-02-20T10:00:00Z"
}
```

**PriceChangedEvent:**
```json
{
    "id": 1, "slug": "iphone-15",
    "oldPrice": 999.00, "newPrice": 899.00,
    "oldDiscountPrice": null, "newDiscountPrice": 799.00,
    "changedAt": "...", "eventId": "...", "timestamp": "..."
}
```

**ProductDeletedEvent:**
```json
{
    "id": 1, "slug": "iphone-15", "sku": "IP15-001",
    "deletedAt": "...", "eventId": "...", "timestamp": "..."
}
```

### Topic: `inventory-events` (produced by Inventory Service)

**StockUpdatedEvent:**
```json
{
    "eventId": "uuid-string",
    "eventType": "STOCK_UPDATED",
    "productId": 1,
    "newQuantity": 95,
    "stockStatus": "AVAILABLE",
    "timestamp": "2026-02-20T10:00:00Z"
}
```

**StockReservedEvent:**
```json
{
    "eventId": "uuid-string",
    "eventType": "STOCK_RESERVED",
    "productId": 1,
    "orderId": "order-uuid",
    "reservedQuantity": 2,
    "remainingQuantity": 93,
    "stockStatus": "AVAILABLE",
    "timestamp": "..."
}
```

---

## SECURITY PATTERN (Shared JWT Secret via Vault)

All services (user-service, product-service, inventory-service) share the same JWT access secret stored in Vault at `secret/user-service → jwt.access.secret`. Each service validates tokens independently — no inter-service call needed for authentication.

Pattern from existing services:
- `JwtUtil.java` — validates access tokens only (no token generation, no refresh token handling)
- `JwtAuthenticationFilter.java` — extracts token from `access_token` cookie OR `Authorization: Bearer` header, validates, sets SecurityContext with userId (UUID) and `ROLE_<role>` authority
- `SecurityConfig.java` — STATELESS sessions, permits public endpoints, role-based method security with `@PreAuthorize`

**The cart service must follow the same security pattern.**

---

## VAULT SECRET STRUCTURE

Each service stores its secrets at `secret/<service-name>`. Secrets are injected via Spring Cloud Vault.

**For cart service, inject:**
```bash
vault kv put secret/cart-service \
  spring.datasource.url="jdbc:postgresql://localhost:5432/cart_service_db" \
  spring.datasource.username="cart_service_db" \
  spring.datasource.password="cart_service_db" \
  spring.data.redis.password=""
```

The cart service must also read JWT secrets from user-service's Vault path:
```yaml
spring:
  config:
    import: "vault://"
  cloud:
    vault:
      uri: http://localhost:8200
      token: root-token
      enabled: true
      fail-fast: true
      kv:
        enabled: true
        backend: secret
        default-context: cart-service
```

**IMPORTANT:** Use `import: "vault://"` with `default-context`, NOT `import: "vault://secret/cart-service"`. The JWT secret (`jwt.access.secret`) is read from Vault at `secret/cart-service` — you must copy it there (same value as `secret/user-service`).

---

## KAFKA CONFIGURATION PATTERN (from existing services)

**Producer uses a custom serializer (NOT deprecated `JsonSerializer`):**
```java
@Bean
public Serializer<Object> jsonSerializer(@Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
    return (topic, data) -> {
        try {
            return kafkaObjectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message", e);
        }
    };
}
```

**Consumer uses a custom deserializer (NOT deprecated `JsonDeserializer`):**
```java
@Bean
public Deserializer<JsonNode> jsonNodeDeserializer() {
    return new Deserializer<>() {
        @Override
        public JsonNode deserialize(String topic, byte[] data) {
            if (data == null) return null;
            try {
                return kafkaObjectMapper.readTree(data);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing to JsonNode", e);
            }
        }
    };
}
```

Both use `ObjectMapper` with `JavaTimeModule` registered. The serializer/deserializer classes from `org.springframework.kafka.support.serializer` (`JsonSerializer`, `JsonDeserializer`) are DEPRECATED in Spring Kafka 4.0 — DO NOT use them.

**IMPORTANT:** When multiple ObjectMapper beans exist (`kafkaObjectMapper`, `redisObjectMapper`), use `@Qualifier` explicitly in constructors. Do NOT use `@RequiredArgsConstructor` for classes that need qualified beans — use explicit constructors instead.

---

## WHAT I NEED YOU TO CREATE: CART SERVICE

### Service Details
- **Name:** cart-service
- **Port:** 8084 (REST)
- **Context path:** /api/v1
- **Base package:** `com.ecommerce.cartservice`
- **Database:** `cart_service_db` (PostgreSQL, same instance)
- **Vault path:** `secret/cart-service`

### Technology Stack (must match existing services exactly)
- Java 21
- Spring Boot 4.0.2
- Gradle 9.3
- Spring Cloud 2025.1.0
- Kafka: spring-boot-starter-kafka (with custom non-deprecated serializer/deserializer)
- Redis: spring-boot-starter-data-redis (for caching cart data)
- PostgreSQL: via spring-boot-starter-data-jpa
- Vault: spring-cloud-starter-vault-config
- JWT: jjwt 0.13.0
- MapStruct: 1.6.3
- Lombok
- Swagger: springdoc-openapi-starter-webmvc-ui 2.8.14

### Cart Service does NOT need gRPC
The cart service is a REST-only service. It does NOT expose gRPC endpoints. It calls Product Service and Inventory Service via gRPC as a CLIENT.

---

### Database Schema

**carts table:**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Unique cart ID |
| user_id | UUID | NOT NULL, UNIQUE, INDEXED | Reference to user (from User Service) — NO foreign key |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, MERGED, CONVERTED, ABANDONED |
| created_at | TIMESTAMP | @CreationTimestamp | |
| updated_at | TIMESTAMP | @UpdateTimestamp | |

**cart_items table:**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | |
| cart_id | BIGINT | NOT NULL, FK to carts, INDEXED | |
| product_id | BIGINT | NOT NULL | Reference to product (from Product Service) — NO foreign key |
| product_name | VARCHAR | NOT NULL | Snapshot at time of adding (denormalized for display) |
| product_image_url | VARCHAR | | Snapshot at time of adding |
| unit_price | DECIMAL(10,2) | NOT NULL | Price snapshot at time of adding |
| quantity | INTEGER | NOT NULL, MIN 1 | Quantity user wants |
| created_at | TIMESTAMP | @CreationTimestamp | |
| updated_at | TIMESTAMP | @UpdateTimestamp | |

**Unique constraint:** (cart_id, product_id) — a product can only appear once per cart; adding same product increases quantity.

**CartStatus enum:**
```java
public enum CartStatus {
    ACTIVE,     // Normal shopping cart
    MERGED,     // Cart was merged after login (guest → user)
    CONVERTED,  // Cart was converted to an order
    ABANDONED   // Cart was abandoned (cleanup)
}
```

---

### Inter-Service Communication

**Cart Service as gRPC CLIENT:**

1. **Product Service (port 9090)** — to validate products and get current data:
   - `GetProduct(productId)` → get product name, price, image, isActive status when adding to cart
   - `GetProductsByIds(productIds)` → bulk fetch product data when loading cart
   - `CheckProductExists(productId)` → verify product exists before adding

2. **Inventory Service (port 9091)** — to check stock availability:
   - `CheckStock(productId, quantity)` → verify stock is available when adding/updating cart items
   - `GetStockInfo(productId)` → get current stock info for cart display

**gRPC Client Configuration:**
```yaml
grpc:
  client:
    product-service:
      address: static://localhost:9090
      negotiation-type: plaintext
    inventory-service:
      address: static://localhost:9091
      negotiation-type: plaintext
```

The cart service needs the proto files from product-service and inventory-service to generate gRPC client stubs. Copy `product.proto` and `inventory.proto` into `cart-service/src/main/proto/`.

**gRPC Client bean usage pattern:**
```java
@GrpcClient("product-service")
private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productStub;

@GrpcClient("inventory-service")
private InventoryGrpcServiceGrpc.InventoryGrpcServiceBlockingStub inventoryStub;
```

---

### Kafka Events

**Cart Service PRODUCES to `cart-events` topic:**

| Event | When | Payload |
|-------|------|---------|
| CART_ITEM_ADDED | When user adds product to cart | `{eventId, eventType, userId, cartId, productId, productName, quantity, unitPrice, timestamp}` |
| CART_ITEM_UPDATED | When user changes quantity | `{eventId, eventType, userId, cartId, productId, oldQuantity, newQuantity, timestamp}` |
| CART_ITEM_REMOVED | When user removes product from cart | `{eventId, eventType, userId, cartId, productId, timestamp}` |
| CART_CLEARED | When user clears entire cart | `{eventId, eventType, userId, cartId, itemCount, timestamp}` |
| CART_CONVERTED | When cart is converted to an order | `{eventId, eventType, userId, cartId, orderId, totalAmount, itemCount, timestamp}` |

**Cart Service CONSUMES from `product-events` topic (group: `cart-service-group`):**

| Event | Action |
|-------|--------|
| PriceChangedEvent | Update `unit_price` in cart_items for affected product (keep cart prices fresh) |
| ProductDeletedEvent | Remove cart items for deleted product, notify user if possible |

**Cart Service CONSUMES from `inventory-events` topic (group: `cart-service-group`):**

| Event | Action |
|-------|--------|
| STOCK_UPDATED (with OUT_OF_STOCK) | Flag cart items for out-of-stock products (don't auto-remove, just mark) |

---

### Redis Usage
- Cache active cart per user: key pattern `cart:{userId}`, TTL 30 minutes
- Use `@Cacheable`, `@CachePut`, `@CacheEvict` annotations
- Cache is evicted when cart is modified (add/update/remove items)
- On read, try cache first; on miss, load from DB and cache

---

### REST Endpoints

All cart endpoints require authentication (user must be logged in). The userId is extracted from the JWT token — users can only access their own cart.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/cart` | CUSTOMER, STORE, ADMIN | Get current user's cart with all items |
| POST | `/cart/items` | CUSTOMER, STORE, ADMIN | Add item to cart |
| PUT | `/cart/items/{productId}` | CUSTOMER, STORE, ADMIN | Update item quantity |
| DELETE | `/cart/items/{productId}` | CUSTOMER, STORE, ADMIN | Remove item from cart |
| DELETE | `/cart` | CUSTOMER, STORE, ADMIN | Clear entire cart |
| GET | `/cart/summary` | CUSTOMER, STORE, ADMIN | Get cart summary (total items, total price) |
| POST | `/cart/validate` | CUSTOMER, STORE, ADMIN | Validate cart (check all items in stock and prices current) |

**Request/Response DTOs:**

```java
// Request: Add item to cart
public record AddToCartRequest(
    @NotNull Long productId,
    @NotNull @Min(1) @Max(99) Integer quantity
) {}

// Request: Update item quantity
public record UpdateCartItemRequest(
    @NotNull @Min(1) @Max(99) Integer quantity
) {}

// Response: Full cart
public record CartResponse(
    Long cartId,
    UUID userId,
    List<CartItemResponse> items,
    BigDecimal totalPrice,
    Integer totalItems,
    CartStatus status,
    Instant updatedAt
) {}

// Response: Single cart item
public record CartItemResponse(
    Long productId,
    String productName,
    String productImageUrl,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal subtotal,       // unitPrice * quantity
    Boolean inStock,           // from inventory check
    Instant addedAt
) {}

// Response: Cart summary (lightweight)
public record CartSummaryResponse(
    Integer totalItems,
    Integer uniqueProducts,
    BigDecimal totalPrice
) {}

// Response: Cart validation result
public record CartValidationResponse(
    Boolean valid,
    List<CartItemValidation> items
) {}

public record CartItemValidation(
    Long productId,
    String productName,
    Boolean available,         // product still exists and is active
    Boolean inStock,           // enough stock available
    Boolean priceChanged,      // price differs from snapshot
    BigDecimal currentPrice,   // current price from product service
    BigDecimal cartPrice,      // price in cart
    Integer requestedQuantity,
    Integer availableQuantity
) {}
```

---

### Core Business Logic

**Add to Cart Flow:**
1. Extract userId from JWT SecurityContext
2. Call Product Service via gRPC: `GetProduct(productId)` — verify product exists, is active, get name/price/image
3. Call Inventory Service via gRPC: `CheckStock(productId, quantity)` — verify stock is available
4. Find or create cart for user (ACTIVE status)
5. If product already in cart → increase quantity (check stock for new total quantity)
6. If product not in cart → create new cart item with price snapshot
7. Evict Redis cache
8. Publish `CART_ITEM_ADDED` event to Kafka
9. Return updated cart

**Update Quantity Flow:**
1. Extract userId from JWT
2. Find cart item
3. Call Inventory Service via gRPC: `CheckStock(productId, newQuantity)` — verify stock for new quantity
4. Update quantity
5. Evict Redis cache
6. Publish `CART_ITEM_UPDATED` event
7. Return updated cart

**Remove Item Flow:**
1. Extract userId from JWT
2. Find and remove cart item
3. If cart is now empty, keep cart as ACTIVE (don't delete)
4. Evict Redis cache
5. Publish `CART_ITEM_REMOVED` event
6. Return updated cart

**Clear Cart Flow:**
1. Extract userId from JWT
2. Remove all items from cart
3. Keep cart entity as ACTIVE
4. Evict Redis cache
5. Publish `CART_CLEARED` event

**Validate Cart Flow (called before checkout):**
1. Extract userId from JWT
2. For each cart item:
   - Call Product Service via gRPC: `GetProduct(productId)` — check exists, isActive, get current price
   - Call Inventory Service via gRPC: `CheckStock(productId, quantity)` — check current availability
   - Compare prices (detect price changes since item was added)
3. Return validation result with per-item status
4. Do NOT auto-modify cart — let the user decide

**Get Cart Flow:**
1. Extract userId from JWT
2. Try Redis cache first (`cart:{userId}`)
3. On miss: load from DB, map to response, cache in Redis
4. Return cart with all items

---

### Project Structure

```
cart-service/
├── build.gradle
├── settings.gradle
├── gradlew, gradlew.bat
├── gradle/wrapper/
├── src/main/
│   ├── java/com/ecommerce/cartservice/
│   │   ├── CartServiceApplication.java
│   │   ├── config/
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   ├── KafkaProducerConfig.java
│   │   │   ├── KafkaTopicConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── controller/
│   │   │   └── CartController.java
│   │   ├── dto/
│   │   │   ├── event/
│   │   │   │   ├── CartItemAddedEvent.java         (PRODUCED)
│   │   │   │   ├── CartItemUpdatedEvent.java        (PRODUCED)
│   │   │   │   ├── CartItemRemovedEvent.java        (PRODUCED)
│   │   │   │   ├── CartClearedEvent.java            (PRODUCED)
│   │   │   │   ├── CartConvertedEvent.java          (PRODUCED)
│   │   │   │   ├── PriceChangedEvent.java           (CONSUMED from Product Service)
│   │   │   │   ├── ProductDeletedEvent.java         (CONSUMED from Product Service)
│   │   │   │   └── StockUpdatedEvent.java           (CONSUMED from Inventory Service)
│   │   │   ├── request/
│   │   │   │   ├── AddToCartRequest.java
│   │   │   │   └── UpdateCartItemRequest.java
│   │   │   ├── response/
│   │   │   │   ├── CartResponse.java
│   │   │   │   ├── CartItemResponse.java
│   │   │   │   ├── CartSummaryResponse.java
│   │   │   │   ├── CartValidationResponse.java
│   │   │   │   └── CartItemValidation.java
│   │   │   └── common/
│   │   │       ├── ErrorResponse.java
│   │   │       └── ValidationErrorResponse.java
│   │   ├── entity/
│   │   │   ├── Cart.java
│   │   │   ├── CartItem.java
│   │   │   └── CartStatus.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── CartNotFoundException.java
│   │   │   ├── CartItemNotFoundException.java
│   │   │   ├── ProductNotAvailableException.java
│   │   │   └── InsufficientStockException.java
│   │   ├── grpc/
│   │   │   ├── ProductGrpcClient.java
│   │   │   └── InventoryGrpcClient.java
│   │   ├── kafka/
│   │   │   ├── CartEventProducer.java
│   │   │   ├── ProductEventConsumer.java
│   │   │   └── InventoryEventConsumer.java
│   │   ├── mapper/
│   │   │   └── CartMapper.java
│   │   ├── repository/
│   │   │   ├── CartRepository.java
│   │   │   └── CartItemRepository.java
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── JwtUtil.java
│   │   └── service/
│   │       └── CartService.java
│   ├── proto/
│   │   ├── product.proto          (copied from product-service)
│   │   └── inventory.proto        (copied from inventory-service)
│   └── resources/
│       └── application.yml
└── src/test/
```

---

### application.yml

```yaml
spring:
  application:
    name: cart-service

  config:
    import: "vault://"

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  datasource:
    url: ${spring.datasource.url}
    username: ${spring.datasource.username}
    password: ${spring.datasource.password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
      password: ${spring.data.redis.password}

  cloud:
    vault:
      uri: http://localhost:8200
      token: root-token
      enabled: true
      fail-fast: true
      kv:
        enabled: true
        backend: secret
        default-context: cart-service

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}
    consumer:
      group-id: cart-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

  web:
    error:
      include-message: always
      include-binding-errors: always

server:
  port: 8084
  servlet:
    context-path: /api/v1

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

jwt:
  issuer: user-service
  access:
    secret: ${jwt.access.secret}

grpc:
  client:
    product-service:
      address: static://localhost:9090
      negotiation-type: plaintext
    inventory-service:
      address: static://localhost:9091
      negotiation-type: plaintext

logging:
  level:
    root: INFO
    com.ecommerce.cartservice: DEBUG
    org.springframework.security: INFO
```

---

### gRPC Client Wrapper Pattern

Wrap gRPC stubs in Spring `@Service` classes for clean separation:

```java
@Service
@Slf4j
public class ProductGrpcClient {

    @GrpcClient("product-service")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productStub;

    public ProductResponse getProduct(Long productId) {
        log.debug("gRPC call: GetProduct({})", productId);
        try {
            return productStub.getProduct(
                GetProductRequest.newBuilder().setProductId(productId).build()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting product {}: {}", productId, e.getStatus());
            throw new ProductNotAvailableException("Product not available: " + productId);
        }
    }

    public List<ProductResponse> getProductsByIds(List<Long> productIds) { ... }
    public boolean checkProductExists(Long productId) { ... }
}
```

```java
@Service
@Slf4j
public class InventoryGrpcClient {

    @GrpcClient("inventory-service")
    private InventoryGrpcServiceGrpc.InventoryGrpcServiceBlockingStub inventoryStub;

    public CheckStockResponse checkStock(Long productId, int quantity) { ... }
    public StockInfoResponse getStockInfo(Long productId) { ... }
}
```

---

### Entity Relationships

```
Cart (1) ──── (N) CartItem
  │                  │
  └─ userId (UUID)   └─ productId (Long, from Product Service)
```

**Cart entity** uses `@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)` for cart items.
**CartItem entity** uses `@ManyToOne(fetch = FetchType.LAZY)` with `@JoinColumn(name = "cart_id")` back to cart.

---

### SecurityConfig for Cart Service

All cart endpoints require authentication. The userId is extracted from the JWT token:

```java
// In SecurityConfig:
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/info",
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
    .requestMatchers("/cart/**").authenticated()
    .anyRequest().authenticated()
)
```

**Extracting userId in controller/service:**
```java
// In controller — get userId from SecurityContext
UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
```

Users can ONLY access their own cart. There is no admin "view other user's cart" endpoint.

---

### Inter-Service Communication Summary

```
┌─────────────────────────────────────────────────────────────────────┐
│                 CART SERVICE DATA FLOW                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. ADD TO CART:                                                    │
│     Cart Service → gRPC → Product Service (GetProduct)              │
│     Cart Service → gRPC → Inventory Service (CheckStock)            │
│     Cart Service → Kafka(cart-events) → [future: analytics]         │
│                                                                     │
│  2. PRICE CHANGE SYNC:                                              │
│     Product Service → Kafka(product-events) → Cart Service          │
│     Cart Service updates cart item prices                           │
│                                                                     │
│  3. PRODUCT DELETED:                                                │
│     Product Service → Kafka(product-events) → Cart Service          │
│     Cart Service removes affected cart items                        │
│                                                                     │
│  4. STOCK EXHAUSTED:                                                │
│     Inventory Service → Kafka(inventory-events) → Cart Service      │
│     Cart Service flags out-of-stock items                           │
│                                                                     │
│  5. VALIDATE CART (before checkout):                                │
│     Cart Service → gRPC → Product Service (GetProduct per item)     │
│     Cart Service → gRPC → Inventory Service (CheckStock per item)   │
│                                                                     │
│  6. CONVERT TO ORDER (future):                                      │
│     Order Service → REST/gRPC → Cart Service (get cart)             │
│     Cart Service marks cart as CONVERTED                            │
│     Cart Service → Kafka(cart-events) → CART_CONVERTED event        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

     user-service (:8081)          product-service (:8082, gRPC :9090)
     ┌───────────────────┐         ┌────────────────────────────────┐
     │ Issues JWT tokens │         │ Products, Categories, Images   │
     │ Auth, Registration│         │ gRPC Server (product data)     │
     └────────┬──────────┘         │ Kafka Producer: product-events │
              │                    └────────────┬───────────────────┘
              │ JWT secret                      │ gRPC (GetProduct,
              │ (Vault)                         │  GetProductsByIds)
              │                                 │ + Kafka events
              ▼                                 ▼
     ┌─────────────┐              ┌────────────────────────────────┐
     │   Vault     │              │  cart-service (:8084)           │
     │ (shared     │              │  Shopping cart management       │
     │  secrets)   │              │  gRPC Client → Product Service  │
     └─────────────┘              │  gRPC Client → Inventory Service│
                                  │  Kafka Producer: cart-events    │
              ▲                   │  Kafka Consumer: product-events │
              │                   │  Kafka Consumer: inventory-events│
              │                   └────────────┬───────────────────┘
              │                                │ gRPC (CheckStock,
              │                                │  GetStockInfo)
              │                                ▼
     ┌────────────────────────────────────────────────────────────┐
     │  inventory-service (:8083, gRPC :9091)                     │
     │  Stock management, Reservations                            │
     │  gRPC Server (stock operations)                            │
     │  Kafka Producer: inventory-events                          │
     └────────────────────────────────────────────────────────────┘
```

---

### CRITICAL RULES

1. **DO NOT use deprecated classes:** `JsonSerializer`, `JsonDeserializer` from `org.springframework.kafka.support.serializer` are deprecated in Spring Kafka 4.0. Use custom serializer/deserializer with ObjectMapper.
2. **DO NOT use `GenericJackson2JsonRedisSerializer`** — it is deprecated. Use custom `RedisSerializer<Object>` with ObjectMapper. Use `ObjectMapper.DefaultTyping.EVERYTHING` for `activateDefaultTyping` to support Java records.
3. **Use records for DTOs** where possible (events, requests, responses).
4. **Use Lombok `@Data`, `@Builder` for entities.**
5. **MapStruct for mappers** with `componentModel = MappingConstants.ComponentModel.SPRING`.
6. **Product ID is Long (BIGINT)**, NOT UUID. User ID is UUID. This must be consistent across services.
7. **Each event has eventId (String UUID) and timestamp (Instant).**
8. **Kafka topic config is a constants class** with `private` constructor, not a `@Configuration` bean.
9. **When multiple ObjectMapper beans exist**, use `@Qualifier("kafkaObjectMapper")` explicitly. Do NOT rely on `@RequiredArgsConstructor` for classes that inject ObjectMapper — use explicit constructors with `@Qualifier`.
10. **application.yml** must follow the exact same patterns as inventory-service (Vault config with `default-context`, Kafka config, Redis config).
11. **No gRPC server** in cart service — only gRPC clients (product-service, inventory-service).
12. **Cart is per-user** — userId from JWT, users can only access their own cart.
13. **Price snapshots** — store price at time of adding to cart. Validate/sync via Kafka events and validate endpoint.

### PLEASE IMPLEMENT THE COMPLETE CART SERVICE

Create all files listed in the project structure above. Make sure:
- `build.gradle` matches existing services' dependency versions exactly (add gRPC client deps, remove gRPC server annotation processor if not needed)
- `application.yml` follows the same patterns as inventory-service
- Proto files are copied from product-service and inventory-service (for client stub generation)
- gRPC client wrappers properly handle errors and map to service exceptions
- Kafka consumer correctly deserializes events from `product-events` and `inventory-events` topics
- Kafka producer correctly serializes cart events for the `cart-events` topic
- Security config follows the same pattern (shared JWT secret from Vault, all cart endpoints require auth)
- Redis caching is configured for cart data with 30-minute TTL
- All service methods have proper logging with `@Slf4j`
- Exception handling follows the same pattern (GlobalExceptionHandler)
- Cart items store price snapshots and product name/image for display
- The `validate` endpoint checks all items via gRPC before checkout
