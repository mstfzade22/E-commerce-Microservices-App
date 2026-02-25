# Prompt: Create Inventory Service for E-Commerce Microservices App

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
- Tokens are sent via `access_token` HTTP cookie

**2. Product Service** (port 8082, gRPC port 9090, context-path: /api/v1)
- Manages products, categories, product images
- PostgreSQL DB: `product_service_db`
- Redis: caches products by ID, by slug, categories by ID, by slug, category tree
- Vault: reads JWT secrets from `secret/user-service` for token validation, DB credentials from `secret/product-service`
- Kafka PRODUCER: publishes to `product-events` topic
- Kafka CONSUMER: listens to `inventory-events` topic
- gRPC SERVER: exposes product data to other services
- MinIO: stores product images
- Swagger: OpenAPI at `/v3/api-docs`, UI at `/swagger-ui`

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

## EXISTING DATABASE SCHEMAS

### Product Service DB (`product_service_db`)

**products table:**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (auto-increment) | PK |
| category_id | BIGINT | FK to categories |
| name | VARCHAR, NOT NULL | |
| slug | VARCHAR, NOT NULL, UNIQUE | indexed |
| price | DECIMAL | |
| discount_price | DECIMAL | |
| description | TEXT | |
| short_description | VARCHAR | |
| sku | VARCHAR | |
| stock_status | VARCHAR(20) | Enum: AVAILABLE, LOW_STOCK, OUT_OF_STOCK |
| is_active | BOOLEAN | |
| is_featured | BOOLEAN | |
| weight_kg | DOUBLE | |
| length_cm, width_cm, height_cm | DOUBLE | |
| attributes | JSONB | |
| created_at | TIMESTAMP | @CreationTimestamp |
| updated_at | TIMESTAMP | @UpdateTimestamp |

**categories table:**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (auto-increment) | PK |
| name | VARCHAR | |
| slug | VARCHAR, UNIQUE | indexed |
| description | TEXT | |
| image_url | VARCHAR | |
| parent_id | BIGINT | self-referencing FK |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**product_images table:**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (auto-increment) | PK |
| product_id | BIGINT, NOT NULL | FK to products |
| image_url | VARCHAR | |
| is_primary | BOOLEAN | default false |
| display_order | INT | default 0 |
| alt_text | VARCHAR | |

**StockStatus enum (in Product Service):**
```java
public enum StockStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus fromQuantity(int quantity) {
        if (quantity <= 0) return OUT_OF_STOCK;
        else if (quantity <= 10) return LOW_STOCK;
        else return AVAILABLE;
    }
}
```

---

## KAFKA TOPICS & EVENTS (Currently Existing)

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

**ProductUpdatedEvent:**
```json
{
    "id": 1, "name": "...", "slug": "...", "sku": "...",
    "price": 899.00, "categoryId": 1, "isActive": true,
    "updatedAt": "...", "eventId": "...", "timestamp": "..."
}
```

**ProductDeletedEvent:**
```json
{
    "id": 1, "slug": "iphone-15", "sku": "IP15-001",
    "deletedAt": "...", "eventId": "...", "timestamp": "..."
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

### Topic: `inventory-events` (to be produced by Inventory Service, consumed by Product Service)

**Product Service already consumes this topic with `InventoryStockUpdatedEvent`:**
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryStockUpdatedEvent(
        String eventId,
        String eventType,
        Long productId,
        Integer newQuantity,
        StockStatus stockStatus,
        Instant timestamp
)
```

The Product Service consumer (`InventoryEventConsumer.java`) listens on `inventory-events` topic, group `product-service-group`, and calls `productService.updateStockStatus(productId, stockStatus)` which updates the `stock_status` field in the products table and refreshes the Redis cache via `@CachePut`.

---

## gRPC (Currently Existing)

### Product Service gRPC Server (port 9090)

**product.proto:**
```protobuf
syntax = "proto3";
package com.ecommerce.productservice.grpc;

service ProductGrpcService {
    rpc GetProduct(GetProductRequest) returns (ProductResponse);
    rpc GetProductsByIds(GetProductsByIdsRequest) returns (ProductListResponse);
    rpc CheckProductExists(CheckProductExistsRequest) returns (CheckProductExistsResponse);
    rpc GetProductStockStatus(GetProductStockStatusRequest) returns (ProductStockStatusResponse);
    rpc GetProductsByCategory(GetProductsByCategoryRequest) returns (ProductListResponse);
}

enum StockStatus {
    STOCK_STATUS_UNSPECIFIED = 0;
    AVAILABLE = 1;
    LOW_STOCK = 2;
    OUT_OF_STOCK = 3;
}
```

Product Service uses `net.devh:grpc-spring-boot-starter:3.1.0.RELEASE` for gRPC. The gRPC service implementation maps ProductService responses to protobuf messages.

---

## SECURITY PATTERN (Shared JWT Secret via Vault)

Both user-service and product-service read the same JWT access secret from Vault (`secret/user-service → jwt.access.secret`). Each service validates tokens independently — no inter-service call needed for authentication.

Product Service has:
- `JwtUtil.java` — validates access tokens only (no token generation, no refresh token handling)
- `JwtAuthenticationFilter.java` — extracts token from `access_token` cookie, validates, sets SecurityContext with userId and `ROLE_<role>`
- `SecurityConfig.java` — STATELESS sessions, GET /products/** and GET /categories/** are public, POST/PUT/DELETE require ADMIN or STORE role

**The inventory service must follow the same security pattern: copy JwtUtil (validation-only) and JwtAuthenticationFilter from product-service, read JWT secret from Vault at `secret/user-service`.**

---

## VAULT SECRET STRUCTURE

Each service stores its secrets at `secret/<service-name>`. Secrets are injected via Spring Cloud Vault.

**For inventory service, inject:**
```bash
vault kv put secret/inventory-service \
  spring.datasource.url="jdbc:postgresql://localhost:5432/inventory_service_db" \
  spring.datasource.username="inventory_service_db" \
  spring.datasource.password="inventory_service_db" \
  spring.data.redis.password=""
```

The inventory service must also import JWT secrets from user-service's Vault path (same as product-service):
```yaml
spring:
  config:
    import:
      - "vault://secret/user-service"
      - "vault://secret/inventory-service"
```

---

## KAFKA CONFIGURATION PATTERN (from Product Service)

**Producer uses a custom serializer (NOT deprecated `JsonSerializer`):**
```java
@Bean
public Serializer<Object> jsonSerializer(ObjectMapper kafkaObjectMapper) {
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
public Deserializer<InventoryStockUpdatedEvent> inventoryEventDeserializer() {
    return new Deserializer<>() {
        @Override
        public InventoryStockUpdatedEvent deserialize(String topic, byte[] data) {
            if (data == null) return null;
            try {
                return kafkaObjectMapper.readValue(data, InventoryStockUpdatedEvent.class);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        }
    };
}
```

Both use `ObjectMapper` with `JavaTimeModule` registered. The serializer/deserializer classes from `org.springframework.kafka.support.serializer` (`JsonSerializer`, `JsonDeserializer`) are DEPRECATED in Spring Kafka 4.0 — DO NOT use them.

---

## WHAT I NEED YOU TO CREATE: INVENTORY SERVICE

### Service Details
- **Name:** inventory-service
- **Port:** 8083 (REST)
- **gRPC port:** 9091
- **Context path:** /api/v1
- **Base package:** `com.ecommerce.inventoryservice`
- **Database:** `inventory_service_db` (PostgreSQL, same instance)
- **Vault path:** `secret/inventory-service`

### Technology Stack (must match product-service exactly)
- Java 21
- Spring Boot 4.0.2
- Gradle 9.3
- Spring Cloud 2025.1.0
- gRPC: grpc-netty-shaded 1.62.2, protobuf 3.25.3, grpc-spring-boot-starter 3.1.0.RELEASE
- Kafka: spring-boot-starter-kafka (with custom non-deprecated serializer/deserializer)
- Redis: spring-boot-starter-data-redis (for caching stock data)
- PostgreSQL: via spring-boot-starter-data-jpa
- Vault: spring-cloud-starter-vault-config
- JWT: jjwt 0.13.0
- MapStruct: 1.6.3
- Lombok
- Swagger: springdoc-openapi-starter-webmvc-ui 2.8.14

### Database Schema

**inventory table:**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Unique inventory record ID |
| product_id | BIGINT | UNIQUE, NOT NULL, INDEXED | Reference to product (from Product Service) — NO foreign key, synced via Kafka |
| quantity | INTEGER | NOT NULL, DEFAULT 0 | Actual stock in warehouse |
| reserved_quantity | INTEGER | NOT NULL, DEFAULT 0 | Stock reserved for pending orders |
| low_stock_threshold | INTEGER | NOT NULL, DEFAULT 10 | When to trigger LOW_STOCK status |
| stock_status | VARCHAR(20) | NOT NULL | AVAILABLE, LOW_STOCK, OUT_OF_STOCK |
| created_at | TIMESTAMP | @CreationTimestamp | |
| updated_at | TIMESTAMP | @UpdateTimestamp | |

**stock_reservations table:**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | |
| product_id | BIGINT | NOT NULL, INDEXED | |
| order_id | VARCHAR | NOT NULL, INDEXED | Reference to order |
| quantity | INTEGER | NOT NULL | Reserved amount |
| status | VARCHAR(20) | NOT NULL | PENDING, CONFIRMED, RELEASED, EXPIRED |
| expires_at | TIMESTAMP | NOT NULL | Auto-release time |
| created_at | TIMESTAMP | @CreationTimestamp | |
| updated_at | TIMESTAMP | @UpdateTimestamp | |

### Kafka Topics & Events

**Inventory Service PRODUCES to `inventory-events` topic:**

| Event | When | Payload |
|-------|------|---------|
| STOCK_UPDATED | After any stock change | `{eventId, eventType: "STOCK_UPDATED", productId, newQuantity, stockStatus, timestamp}` |
| STOCK_RESERVED | When order reserves stock | `{eventId, eventType: "STOCK_RESERVED", productId, orderId, reservedQuantity, remainingQuantity, stockStatus, timestamp}` |
| STOCK_RELEASED | When reservation cancelled/expired | `{eventId, eventType: "STOCK_RELEASED", productId, orderId, releasedQuantity, remainingQuantity, stockStatus, timestamp}` |
| STOCK_CONFIRMED | When payment succeeds | `{eventId, eventType: "STOCK_CONFIRMED", productId, orderId, confirmedQuantity, remainingQuantity, stockStatus, timestamp}` |

**IMPORTANT:** The `STOCK_UPDATED` event MUST match the structure that Product Service's `InventoryStockUpdatedEvent` expects:
```java
public record InventoryStockUpdatedEvent(
    String eventId,
    String eventType,     // "STOCK_UPDATED"
    Long productId,       // MUST be Long to match product.id
    Integer newQuantity,
    StockStatus stockStatus,  // AVAILABLE, LOW_STOCK, OUT_OF_STOCK
    Instant timestamp
)
```

**Inventory Service CONSUMES from `product-events` topic:**

| Event | Action |
|-------|--------|
| ProductCreatedEvent | Create inventory record with initialStock quantity |
| ProductDeletedEvent | Delete/deactivate inventory record |

### gRPC — Inventory Service as SERVER (port 9091)

**inventory.proto:**
```protobuf
service InventoryGrpcService {
    rpc CheckStock(CheckStockRequest) returns (CheckStockResponse);
    rpc ReserveStock(ReserveStockRequest) returns (ReserveStockResponse);
    rpc ReleaseStock(ReleaseStockRequest) returns (ReleaseStockResponse);
    rpc ConfirmStock(ConfirmStockRequest) returns (ConfirmStockResponse);
    rpc GetStockInfo(GetStockInfoRequest) returns (StockInfoResponse);
}
```

These gRPC methods will be called by Order Service (future) for synchronous, real-time stock operations during checkout.

### Redis Usage
- Cache stock info per product: key pattern `inventory:{productId}`, TTL 5 minutes
- Use `@Cacheable`, `@CachePut`, `@CacheEvict` annotations like product-service

### REST Endpoints (for admin/debugging + Swagger testing)
- `GET /inventory/{productId}` — Get stock info (public or authenticated)
- `PUT /inventory/{productId}/stock` — Update stock quantity (ADMIN only)
- `POST /inventory/{productId}/reserve` — Reserve stock (for testing, normally called via gRPC)
- `POST /inventory/{productId}/release` — Release reservation (ADMIN only)
- `GET /inventory/low-stock` — Get products with low stock (ADMIN only)

### Stock Status Logic
When stock changes, calculate status:
```
quantity <= 0                  → OUT_OF_STOCK
quantity > 0 && quantity <= lowStockThreshold → LOW_STOCK
quantity > lowStockThreshold   → AVAILABLE
```

After each stock change:
1. Update inventory record in DB
2. Update Redis cache
3. Publish `STOCK_UPDATED` event to `inventory-events` Kafka topic → Product Service consumes this and updates `product.stock_status`

### Project Structure
```
inventory-service/
├── build.gradle
├── settings.gradle
├── gradlew, gradlew.bat
├── gradle/wrapper/
├── src/main/
│   ├── java/com/ecommerce/inventoryservice/
│   │   ├── InventoryServiceApplication.java
│   │   ├── config/
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   ├── KafkaProducerConfig.java
│   │   │   ├── KafkaTopicConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── controller/
│   │   │   └── InventoryController.java
│   │   ├── dto/
│   │   │   ├── event/
│   │   │   │   ├── StockUpdatedEvent.java          (PRODUCED)
│   │   │   │   ├── StockReservedEvent.java          (PRODUCED)
│   │   │   │   ├── StockReleasedEvent.java          (PRODUCED)
│   │   │   │   ├── StockConfirmedEvent.java         (PRODUCED)
│   │   │   │   ├── ProductCreatedEvent.java         (CONSUMED from Product Service)
│   │   │   │   └── ProductDeletedEvent.java         (CONSUMED from Product Service)
│   │   │   ├── request/
│   │   │   │   ├── StockUpdateRequest.java
│   │   │   │   └── StockReservationRequest.java
│   │   │   ├── response/
│   │   │   │   ├── InventoryResponse.java
│   │   │   │   └── StockStatusResponse.java
│   │   │   └── common/
│   │   │       ├── ErrorResponse.java
│   │   │       └── ValidationErrorResponse.java
│   │   ├── entity/
│   │   │   ├── Inventory.java
│   │   │   ├── StockReservation.java
│   │   │   ├── StockStatus.java                     (same enum as Product Service)
│   │   │   └── ReservationStatus.java               (PENDING, CONFIRMED, RELEASED, EXPIRED)
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── InsufficientStockException.java
│   │   │   ├── InventoryNotFoundException.java
│   │   │   ├── DuplicateResourceException.java
│   │   │   └── InvalidRequestException.java
│   │   ├── grpc/
│   │   │   ├── InventoryGrpcServiceImpl.java
│   │   │   └── GrpcExceptionAdvice.java
│   │   ├── kafka/
│   │   │   ├── InventoryEventProducer.java
│   │   │   └── ProductEventConsumer.java
│   │   ├── mapper/
│   │   │   └── InventoryMapper.java
│   │   ├── repository/
│   │   │   ├── InventoryRepository.java
│   │   │   └── StockReservationRepository.java
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java         (copied from product-service, adapted)
│   │   │   └── JwtUtil.java                         (copied from product-service, validation only)
│   │   └── service/
│   │       ├── InventoryService.java
│   │       └── StockReservationService.java
│   ├── proto/
│   │   └── inventory.proto
│   └── resources/
│       └── application.yml
└── src/test/
```

### Inter-Service Communication Summary

```
┌─────────────────────────────────────────────────────────────────────┐
│                    COMPLETE DATA FLOW                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. PRODUCT CREATED:                                                │
│     Product Service → Kafka(product-events) → Inventory Service     │
│     Inventory Service creates inventory record with initialStock    │
│                                                                     │
│  2. STOCK STATUS SYNC:                                              │
│     Inventory Service → Kafka(inventory-events) → Product Service   │
│     Product Service updates product.stockStatus + Redis cache       │
│                                                                     │
│  3. CHECKOUT (future - Order Service):                              │
│     Order Service → gRPC → Inventory Service (CheckStock)           │
│     Order Service → gRPC → Inventory Service (ReserveStock)         │
│     (synchronous, real-time, ~5ms latency)                          │
│                                                                     │
│  4. PRODUCT DATA LOOKUP (future - Order Service):                   │
│     Order Service → gRPC → Product Service (GetProduct)             │
│     (get price, name, etc. for order)                               │
│                                                                     │
│  5. PRODUCT DELETED:                                                │
│     Product Service → Kafka(product-events) → Inventory Service     │
│     Inventory Service deletes/deactivates inventory record          │
│                                                                     │
│  6. JWT VALIDATION (all services):                                  │
│     Each service reads same JWT secret from Vault                   │
│     Each service validates token independently                      │
│     No inter-service call for auth                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

     user-service (:8081)          product-service (:8082, gRPC :9090)
     ┌───────────────────┐         ┌────────────────────────────────┐
     │ Issues JWT tokens │         │ Products, Categories, Images   │
     │ Auth, Registration│         │ gRPC Server (product data)     │
     │ User management   │         │ Kafka Producer: product-events │
     └────────┬──────────┘         │ Kafka Consumer: inventory-events│
              │                    └────────────┬───────────────────┘
              │ JWT secret                      │
              │ (Vault)                         │ Kafka events
              │                                 │ (bidirectional)
              ▼                                 ▼
     ┌─────────────┐              ┌────────────────────────────────┐
     │   Vault     │              │ inventory-service (:8083,      │
     │ (shared     │              │                  gRPC :9091)   │
     │  secrets)   │              │ Stock management               │
     └─────────────┘              │ Reservations                   │
                                  │ gRPC Server (stock operations) │
                                  │ Kafka Producer: inventory-events│
                                  │ Kafka Consumer: product-events │
                                  └────────────────────────────────┘
```

### CRITICAL RULES

1. **DO NOT use deprecated classes:** `JsonSerializer`, `JsonDeserializer` from `org.springframework.kafka.support.serializer` are deprecated in Spring Kafka 4.0. Use custom serializer/deserializer with ObjectMapper.
2. **DO NOT use `GenericJackson2JsonRedisSerializer`** — it is deprecated. Use custom `RedisSerializer<Object>` with ObjectMapper (same pattern as product-service's RedisConfig).
3. **Use records for DTOs** where possible (events, requests, responses).
4. **Use Lombok `@Data`, `@Builder` for entities.**
5. **MapStruct for mappers** with `componentModel = MappingConstants.ComponentModel.SPRING`.
6. **Product ID is Long (BIGINT)**, NOT UUID. This must be consistent across services.
7. **Each event has eventId (String UUID) and timestamp (Instant).**
8. **Kafka topic config is a constants class** with `private` constructor, not a `@Configuration` bean.
9. **gRPC uses `net.devh:grpc-spring-boot-starter`** with `@GrpcService` and `@GrpcAdvice` annotations.
10. **application.yml** must follow the exact same patterns as product-service (Vault import order, Kafka config, Redis config, etc.)

### PLEASE IMPLEMENT THE COMPLETE INVENTORY SERVICE
Create all files listed in the project structure above. Make sure:
- build.gradle matches product-service's dependency versions exactly
- application.yml follows the same patterns
- Kafka consumer correctly deserializes ProductCreatedEvent and ProductDeletedEvent
- Kafka producer correctly serializes stock events for Product Service to consume
- gRPC proto file is complete with all request/response messages
- Security config follows the same pattern (shared JWT secret from Vault)
- Redis caching is configured for stock data
- All service methods have proper logging with `@Slf4j`
- Exception handling follows the same pattern (GlobalExceptionHandler + GrpcExceptionAdvice)

