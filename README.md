# E-Commerce Microservices Application

A cloud-native e-commerce platform built with microservices architecture using Spring Boot 4.x, gRPC, Apache Kafka, Redis, PostgreSQL, and React 19.

## Architecture Overview

```
                                    ┌──────────────┐
                                    │   Frontend   │
                                    │  React 19    │
                                    └──────┬───────┘
                                           │
                                  ┌────────┴────────┐
                                  │   API Gateway   │
                                  │  Spring Cloud   │
                                  │  JWT + Rate     │
                                  │  Limiting       │
                                  └────────┬────────┘
                                           │
         ┌───────────┬───────────┬─────────┼───────────┬──────────────┬──────────────┐
         ▼           ▼           ▼         ▼           ▼              ▼              ▼
  ┌─────────────┐┌──────────┐┌────────┐┌───────┐┌──────────┐┌────────────┐┌──────────────┐
  │user-service ││ product  ││  cart  ││ order ││ payment  ││ inventory  ││ notification │
  │   :8081     ││ :8082    ││ :8084  ││ :8085 ││  :8086   ││   :8083    ││    :8087     │
  │             ││ gRPC:9090││        ││       ││          ││ gRPC:9091  ││   SSE :8087  │
  └──────┬──────┘└────┬─────┘└───┬────┘└───┬───┘└────┬─────┘└─────┬─────┘└──────┬───────┘
         │            │          │         │         │             │             │
         │       ┌────┴──────────┴─────────┴─────────┴─────────────┴─────────────┘
         │       │    Apache Kafka (KRaft) - Event-Driven Communication
         │       │    Outbox Pattern │ DLQ │ Idempotent Consumers
         │       │    product-events │ inventory-events │ order-events │ payment-events
         │       └──────────────────────────────────────────────────────
         │
    ┌────┴────┐  ┌─────────┐  ┌───────────────────────────────────┐  ┌───────┐  ┌─────────────┐
    │  Vault  │  │PostgreSQL│  │ Redis 7                           │  │ MinIO │  │ Kapital Bank│
    │ :8200   │  │  :5432   │  │ Caching │ Sessions │ Rate Limit  │  │ :9000 │  │  (external) │
    └─────────┘  └─────────┘  └───────────────────────────────────┘  └───────┘  └─────────────┘
```

## Key Features

- **Event-driven architecture** with Apache Kafka (KRaft mode) for asynchronous inter-service communication
- **Transactional Outbox Pattern** ensuring guaranteed event delivery with database-level atomicity
- **Dead Letter Queues (DLQ)** with exponential backoff retry and status tracking (FAILED/RESOLVED/EXHAUSTED)
- **Idempotent consumers** with `processed_events` table and unique constraint deduplication
- **gRPC communication** via Protocol Buffers for low-latency synchronous calls (product/inventory lookups)
- **Redis caching** across all services with per-entity TTLs (products: 1h, inventory: 5m, cart: 30m, orders: 1h, categories: 6h)
- **Redis rate limiting** at the API Gateway level with user/IP-based key resolution
- **Redis token denylist** for JWT revocation on logout
- **Stock reservation system** with reserve/confirm/release lifecycle tied to order and payment status (15-minute timeout)
- **Kapital Bank payment integration** with hosted payment page, callback handling, and auto-refund
- **Payment test mode** for local development without real bank credentials
- **Server-Sent Events (SSE)** for real-time notifications to the frontend with heartbeat keep-alive
- **JWT authentication** with HttpOnly cookies, refresh token rotation, and Redis-based denylist
- **Role-based access control** (CUSTOMER, STORE, ADMIN) across all services
- **Database-per-service** pattern with isolated PostgreSQL instances
- **Email notifications** via Kafka events with Thymeleaf templates and scheduled retry (max 3 attempts)
- **MinIO object storage** for product image uploads (S3-compatible)
- **HashiCorp Vault** for centralized secret management (KV v2, per-service paths)
- **Swagger UI** on every service for API documentation
- **Multi-stage Docker builds** with JVM tuning and non-root containers
- **Docker Compose profiles** for flexible deployment (core, shopping, full, debug, frontend)

## Services

| Service | Port | Description |
|:--------|:-----|:------------|
| **api-gateway** | 8080 | Spring Cloud Gateway: routing, JWT validation, rate limiting, CORS |
| **user-service** | 8081 | Authentication, registration, JWT token management, session tracking |
| **product-service** | 8082, 9090 (gRPC) | Product & category CRUD, image upload via MinIO, gRPC server |
| **inventory-service** | 8083, 9091 (gRPC) | Stock management, reservations (reserve/confirm/release), gRPC server |
| **cart-service** | 8084 | Shopping cart with real-time stock validation via gRPC |
| **order-service** | 8085 | Order lifecycle, stock reservation via gRPC, cart integration via REST |
| **payment-service** | 8086 | Kapital Bank payment gateway integration with test mode |
| **notification-service** | 8087 | Email notifications + SSE real-time events for order, payment, and inventory |

## Technology Stack

| Category | Technology |
|:---------|:-----------|
| **Framework** | Spring Boot 4.0.2 (3.2.5 for user-service), Spring Cloud 2025.1.0, Spring Security |
| **Language** | Java 21 |
| **API** | REST, gRPC 1.62.2 (inter-service), Protocol Buffers 3.25.3 |
| **Database** | PostgreSQL 16 (per-service databases, JSONB columns, HikariCP pooling) |
| **Caching** | Redis 7 (@Cacheable/@CachePut/@CacheEvict, per-entity TTLs, Lettuce client) |
| **Messaging** | Apache Kafka 3.7 (KRaft, Outbox Pattern, DLQ, Idempotency) |
| **Payment** | Kapital Bank E-Commerce API (BasicAuth, JSON, callback-driven) |
| **Object Storage** | MinIO (S3-compatible, product images) |
| **Secrets** | HashiCorp Vault 1.15 (KV v2, per-service paths, fail-fast) |
| **Real-time** | Server-Sent Events (SSE) with heartbeat keep-alive |
| **Email** | Spring Mail + Thymeleaf templates + scheduled retry |
| **Mapping** | MapStruct 1.6.3 (DTO/entity conversion) |
| **Documentation** | OpenAPI 3.0 / Swagger UI 2.8.14 |
| **Build** | Gradle 9.3 |
| **Containerization** | Docker multi-stage builds (eclipse-temurin:21), Docker Compose with profiles |
| **Frontend** | React 19, Vite 8, Tailwind CSS 4, TanStack React Query 5, Zod 4, React Hook Form 7 |

## Kafka Event Architecture

### Topics & Events

| Topic | Producer | Consumers | Events |
|:------|:---------|:----------|:-------|
| `product-events` | product-service | inventory, notification | ProductCreated, ProductUpdated, ProductDeleted, PriceChanged |
| `inventory-events` | inventory-service | product, cart, notification | StockUpdated, StockReserved, StockReleased, StockConfirmed |
| `order-events` | order-service | payment, notification | OrderCreated, OrderConfirmed, OrderCancelled, OrderShipped, OrderDelivered |
| `payment-events` | payment-service | order, cart, notification | PaymentInitiated, PaymentSuccess, PaymentFailed, PaymentRefunded |

### Reliability Patterns

#### Transactional Outbox Pattern
Events are written to an `outbox_events` table within the same database transaction as the business operation. A scheduled publisher (every 500ms) polls for unpublished events and sends them to Kafka, ensuring no events are lost even if Kafka is temporarily unavailable. Published events are cleaned up daily at 3 AM (older than 7 days).

```
Business Operation + Outbox Write (single transaction)
         ↓
Outbox Publisher (@Scheduled, 500ms) → Kafka Topic
         ↓
Mark as published
```

#### Dead Letter Queue (DLQ)
Failed events are persisted to a `failed_events` table with full error context (stack trace, consumer group, partition, offset). Retry uses exponential backoff (`2^retryCount * 120` seconds) with a maximum of 5 retries. Events transition through FAILED → RESOLVED or EXHAUSTED states. Retry scheduler runs every 60 seconds.

#### Idempotent Consumers
A `processed_events` table with a unique constraint on `event_id` prevents duplicate processing. Before handling an event, consumers attempt to insert the event ID — if a `DataIntegrityViolationException` occurs, the event is silently skipped.

#### Consumer Error Handling
Kafka consumers use `DefaultErrorHandler` with `FixedBackOff(1000ms, 3 retries)` for local retries before delegating to the DLQ service. Manual acknowledgment (`AckMode.RECORD`) ensures no event is lost.

## Redis Caching Strategy

Redis serves multiple roles across the platform:

| Role | Service | Details |
|:-----|:--------|:--------|
| **Product Cache** | product-service | `product:id` (1h TTL), `product:slug` (1h TTL) |
| **Category Cache** | product-service | `category:id` (6h), `category:slug` (6h), `category:tree` (6h) |
| **Inventory Cache** | inventory-service | `inventory` by productId (5m TTL) — evicted on reserve/release/confirm |
| **Cart Cache** | cart-service | `cart` by userId (30m TTL) — updated on add/remove/clear |
| **Order Cache** | order-service | `order:id` (1h), `order:user` (15m) — evicted on status changes |
| **Payment Cache** | payment-service | `payment:id` (1h), `payment:order` (30m) |
| **User Cache** | user-service | `user:profile` (30m) |
| **User Info Cache** | notification-service | `user-info` (30m) — cached from user-service lookups |
| **Token Denylist** | user-service | Revoked JWT tokens with TTL matching token expiry |
| **Session Management** | user-service | Active session tracking |
| **Rate Limiting** | api-gateway | Request rate limiting with user/IP key resolution |

All caches use `GenericJackson2JsonRedisSerializer` with polymorphic type validation. Cache operations use Spring's `@Cacheable`, `@CachePut`, and `@CacheEvict` annotations. TTLs are tuned by data volatility — inventory (5m) changes frequently while categories (6h) are relatively static.

## Stock Reservation Lifecycle

```
Cart: Add Item
    ↓
gRPC → Inventory: CheckStock → AVAILABLE / LOW_STOCK / OUT_OF_STOCK
    ↓
Order Created → gRPC → Inventory: ReserveStock (15-min timeout)
    ↓
Payment Success → Inventory: ConfirmStock (permanent deduction)
    ↓
Order Cancelled / Payment Failed → Inventory: ReleaseStock (return to available)
```

Available-to-Promise (ATP) = `quantity - reservedQuantity`

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Option 1: Run Everything in Docker

```bash
git clone https://github.com/mstfzade22/E-commerce-Microservices-App.git
cd E-commerce-Microservices-App

# Full stack (all services + frontend)
docker compose --profile full up -d

# Or core services only
docker compose --profile core up -d

# Or core + shopping services
docker compose --profile shopping up -d

# Add debug tools (Redpanda Console + Redis Insight)
docker compose --profile full --profile debug up -d
```

This starts all infrastructure and services with proper dependency ordering, health checks, and Vault auto-initialization.

### Option 2: Infrastructure in Docker, Services Locally

```bash
# Start infrastructure only
docker compose up -d postgres redis kafka vault vault-init minio

# Optionally add debug UIs
docker compose --profile debug up -d redpanda-console redis-insight

# Run services in order (each in a separate terminal)
./ms-user/gradlew -p ms-user bootRun
./ms-product/gradlew -p ms-product bootRun
./ms-inventory/gradlew -p ms-inventory bootRun
./ms-cart/gradlew -p ms-cart bootRun
./ms-order/gradlew -p ms-order bootRun
./ms-payment/gradlew -p ms-payment bootRun
./ms-notification/gradlew -p ms-notification bootRun
```

> The terminal may stay at 80-87% EXECUTING -- this is normal for Spring Boot. Check logs for `Started ... Application`.

### Create MinIO Bucket (first run only)

```bash
# Via MinIO Console at http://localhost:9001 (minioadmin/minioadmin)
# Create bucket: product-images

# Or via CLI
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/product-images
mc anonymous set download local/product-images
```

## Testing the Payment Flow

Payment test mode is enabled by default (`PAYMENT_TEST_MODE=true`), allowing you to simulate payments without real bank credentials.

```bash
# 1. Register & login to get JWT cookies
# 2. Add items to cart
# 3. Create order:     POST /api/v1/orders
# 4. Confirm order:    PUT  /api/v1/orders/{orderNumber}/confirm
# 5. Initiate payment: POST /api/v1/payments/initiate?amount=40
#    -> Response includes paymentUrl and kapitalOrderId
# 6. Simulate successful payment via callback:
curl "http://localhost:8086/api/v1/payments/callback/result?ID={kapitalOrderId}&STATUS=FullyPaid"
# 7. Verify: payment -> APPROVED, order -> PROCESSING, cart -> cleared
```

For failed payment simulation, use `STATUS=Declined` or `STATUS=Cancelled` in step 6.

Set `PAYMENT_TEST_MODE=false` for production to enable real Kapital Bank verification.

## API Endpoints

### User Service (Port 8081)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/auth/register` | POST | Public | Register new user |
| `/api/v1/auth/login` | POST | Public | Login, returns JWT in HttpOnly cookies |
| `/api/v1/auth/refresh` | POST | Public | Refresh access token (rotates refresh token) |
| `/api/v1/auth/logout` | POST | Auth | Revoke tokens, add to Redis denylist |
| `/api/v1/auth/validate` | GET | Auth | Validate current token |
| `/api/v1/users/me` | GET | Auth | Current user profile (cached 30m) |
| `/api/v1/users/{id}` | GET | Auth | Get user by ID |
| `/api/v1/users/me` | PUT | Auth | Update profile |
| `/api/v1/users/{id}/lock` | POST | ADMIN | Lock user account |
| `/api/v1/users/{id}/unlock` | POST | ADMIN | Unlock user account |

### Product Service (Port 8082)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/products` | GET | Public | List products (paginated, cached) |
| `/api/v1/products/{id}` | GET | Public | Get product by ID (cached 1h) |
| `/api/v1/products/slug/{slug}` | GET | Public | Get product by slug (cached 1h) |
| `/api/v1/products/search?keyword=` | GET | Public | Search products |
| `/api/v1/products/featured` | GET | Public | Featured products |
| `/api/v1/products/category/{id}` | GET | Public | Products by category |
| `/api/v1/products` | POST | ADMIN/STORE | Create product (auto-creates inventory, publishes via outbox) |
| `/api/v1/products/{id}` | PUT | ADMIN/STORE | Update product (cache evict + outbox event) |
| `/api/v1/products/{id}` | DELETE | ADMIN/STORE | Soft delete product |
| `/api/v1/products/{id}/images` | POST | ADMIN/STORE | Upload image to MinIO |
| `/api/v1/categories` | GET | Public | List root categories |
| `/api/v1/categories/tree` | GET | Public | Full category hierarchy (cached 6h) |
| `/api/v1/categories` | POST | ADMIN | Create category |

### Inventory Service (Port 8083)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/inventory/{productId}` | GET | Public | Get stock info (cached 5m) |
| `/api/v1/inventory/{productId}/status` | GET | Public | Simplified stock status |
| `/api/v1/inventory/{productId}/stock` | PUT | ADMIN | Update stock quantity (cache evict) |
| `/api/v1/inventory/{productId}/reserve` | POST | ADMIN/STORE | Reserve stock (cache evict) |
| `/api/v1/inventory/{productId}/release` | POST | ADMIN | Release reservation (cache evict) |
| `/api/v1/inventory/low-stock` | GET | ADMIN | Low stock products |

### Cart Service (Port 8084)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/cart` | GET | Auth | Get cart (cached 30m) |
| `/api/v1/cart/items` | POST | Auth | Add item (gRPC stock check + cache update) |
| `/api/v1/cart/items/{productId}` | PUT | Auth | Update quantity |
| `/api/v1/cart/items/{productId}` | DELETE | Auth | Remove item |
| `/api/v1/cart` | DELETE | Auth | Clear cart |
| `/api/v1/cart/validate` | POST | Auth | Validate cart before checkout |
| `/api/v1/cart/summary` | GET | Auth | Cart summary |

### Order Service (Port 8085)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/orders` | POST | Auth | Create order from cart (gRPC stock reservation, outbox event) |
| `/api/v1/orders` | GET | Auth | Get user's orders (paginated, cached 15m) |
| `/api/v1/orders/{orderNumber}` | GET | Auth | Get order details (cached 1h) |
| `/api/v1/orders/{orderNumber}/confirm` | POST | Auth | Confirm order (outbox event) |
| `/api/v1/orders/{orderNumber}/cancel` | POST | Auth | Cancel order (releases stock, outbox event) |
| `/api/v1/orders/{orderNumber}/process` | POST | STORE/ADMIN | Mark processing |
| `/api/v1/orders/{orderNumber}/ship` | POST | STORE/ADMIN | Mark shipped |
| `/api/v1/orders/{orderNumber}/deliver` | POST | STORE/ADMIN | Mark delivered |
| `/api/v1/orders/{orderNumber}/history` | GET | Auth | Status change history |

### Payment Service (Port 8086)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/payments/initiate?amount=` | POST | Auth | Initiate payment, returns Kapital Bank URL (outbox event) |
| `/api/v1/payments/{id}` | GET | Auth | Get payment details (cached 1h) |
| `/api/v1/payments/order/{orderNumber}` | GET | Auth | Get payment by order (cached 30m) |
| `/api/v1/payments/my` | GET | Auth | User's payments (paginated) |
| `/api/v1/payments/{id}/history` | GET | Auth | Payment status history |
| `/api/v1/payments/{id}/refund` | POST | ADMIN | Refund payment (outbox event) |
| `/api/v1/payments/callback/result` | GET | Public | Kapital Bank callback |

### Notification Service (Port 8087)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/notifications` | GET | Auth | User's notifications (paginated) |
| `/api/v1/notifications/stream` | GET | Auth | SSE stream (real-time notifications) |
| `/api/v1/notifications/failed` | GET | ADMIN | Failed notifications |
| `/api/v1/notifications/{id}/retry` | POST | ADMIN | Retry failed notification |

### gRPC Services

| Service | Port | Methods |
|:--------|:-----|:--------|
| **Product gRPC** | 9090 | GetProduct, GetProductsByIds, CheckProductExists, GetProductStockStatus, GetProductsByCategory |
| **Inventory gRPC** | 9091 | CheckStock, ReserveStock, ReleaseStock, ConfirmStock, GetStockInfo |

## Scheduled Tasks

| Task | Interval | Service | Purpose |
|:-----|:---------|:--------|:--------|
| Outbox Publisher | 500ms | product, inventory, order, payment | Publish pending outbox events to Kafka |
| DLQ Retry | 60s | all consumers | Retry failed events with exponential backoff |
| Outbox Cleanup | Daily 3 AM | product, inventory, order, payment | Remove published events older than 7 days |
| Email Retry | 15 min | notification | Retry failed email notifications (max 3 attempts) |
| SSE Heartbeat | 30s | notification | Keep SSE connections alive |

## Database Schema

Each service has its own PostgreSQL database:

| Database | Tables |
|:---------|:-------|
| `user_service_db` | users, sessions, refresh_tokens |
| `product_service_db` | products, categories, product_images, outbox_events, failed_events, processed_events |
| `inventory_service_db` | inventory, stock_reservations, outbox_events, failed_events, processed_events |
| `cart_service_db` | carts, cart_items, failed_events |
| `order_service_db` | orders, order_items, order_status_history, outbox_events, failed_events, processed_events |
| `payment_service_db` | payments, payment_status_history, outbox_events, failed_events, processed_events |
| `notification_service_db` | notifications, failed_events |

## Security

- **Authentication:** JWT (HS512) with access (1h) / refresh (7d) token pair
- **Token Delivery:** HttpOnly secure cookies (`access_token`, `refresh_token`, SameSite=Strict)
- **Token Rotation:** Refresh tokens are rotated on use, old tokens are revoked
- **Token Revocation:** Redis-based denylist with TTL matching token expiry
- **Gateway Validation:** JWT validated at API Gateway level before routing to services
- **Rate Limiting:** Redis-backed, per-user/IP request throttling at gateway
- **Secret Management:** HashiCorp Vault (KV v2, per-service paths, fail-fast on connection error)
- **Password Hashing:** BCrypt
- **Roles:** CUSTOMER, STORE, ADMIN with `@PreAuthorize` enforcement
- **CORS:** Configured at gateway (allowed origins, credentials enabled)

## Frontend Architecture

Built with **React 19** + **Vite 8** + **Tailwind CSS 4**:

- **Server State:** TanStack React Query 5 for data fetching, caching, and synchronization
- **Forms:** React Hook Form 7 with Zod 4 schema validation
- **Routing:** React Router 7 with dynamic imports
- **HTTP:** Axios with auth interceptors and queue-based token refresh
- **Real-time:** SSE integration with auto-reconnect and exponential backoff
- **Notifications:** React Hot Toast for in-app alerts
- **Icons:** Lucide React
- **Auth:** Context-based auth state with session expiry events
- **SSE Events:** ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED, PAYMENT_SUCCESS, PAYMENT_FAILED, PAYMENT_REFUNDED, LOW_STOCK_ALERT — each invalidates relevant React Query caches

## Docker Infrastructure

| Service | Image | Ports | UI |
|:--------|:------|:------|:---|
| PostgreSQL | `postgres:16` | 5432 | - |
| Redis | `redis:7-alpine` | 6379 | - |
| Kafka (KRaft) | `apache/kafka:3.7.0` | 9092, 29092 | - |
| Vault | `hashicorp/vault:1.15` | 8200 | http://localhost:8200 |
| MinIO | `minio/minio:latest` | 9000, 9001 | http://localhost:9001 |
| Redpanda Console | `redpandadata/console` | 8090 | http://localhost:8090 (debug profile) |
| Redis Insight | `redis/redisinsight` | 5540 | http://localhost:5540 (debug profile) |

### Docker Compose Profiles

| Profile | Services |
|:--------|:---------|
| `core` | Infrastructure + API Gateway + User + Product + Inventory |
| `shopping` | Core + Cart + Order + Payment + Notification |
| `full` | All services + Frontend |
| `debug` | Redpanda Console + Redis Insight |
| `frontend` | Frontend only |

### Container Optimization

- **Multi-stage builds:** Builder (eclipse-temurin:21-jdk) + Runtime (eclipse-temurin:21-jre-alpine)
- **Non-root execution:** `spring:spring` user
- **JVM tuning:** `-Xmx256m-768m`, `-Xss512k`, `-XX:MaxMetaspaceSize=150-256m`, `-XX:+UseSerialGC`
- **Health checks:** HTTP GET to `/actuator/health` with dependency ordering
- **Resource limits:** Per-container memory allocation (64m-896m)

### Connection Pooling

| Pool | Setting |
|:-----|:--------|
| HikariCP (PostgreSQL) | max-pool: 10, min-idle: 5, connection-timeout: 30s, idle-timeout: 10m, max-lifetime: 30m |
| Lettuce (Redis) | max-active: 8, max-idle: 8, min-idle: 2, timeout: 3s |

## API Documentation (Swagger)

| Service | URL |
|:--------|:----|
| User Service | http://localhost:8081/api/v1/swagger-ui.html |
| Product Service | http://localhost:8082/api/v1/swagger-ui.html |
| Inventory Service | http://localhost:8083/api/v1/swagger-ui.html |
| Cart Service | http://localhost:8084/api/v1/swagger-ui.html |
| Order Service | http://localhost:8085/api/v1/swagger-ui.html |
| Payment Service | http://localhost:8086/api/v1/swagger-ui.html |
| Notification Service | http://localhost:8087/api/v1/swagger-ui.html |

## Monitoring

| Tool | URL | Purpose |
|:-----|:----|:--------|
| Actuator Health | `/api/v1/actuator/health` on each service | Health checks |
| Redpanda Console | http://localhost:8090 | Kafka topics, messages, consumer groups |
| Redis Insight | http://localhost:5540 | Redis data browser, cache inspection |
| MinIO Console | http://localhost:9001 | Object storage browser |
| Vault UI | http://localhost:8200 | Secrets management |

## Project Structure

```
E-commerce-Microservices-App/
├── docker-compose.yml          # 16 services with profiles
├── init-databases.sql          # PostgreSQL DDL (creates 7 DBs + users)
├── vault-init.sh               # Vault KV v2 secret initialization
├── README.md
│
├── ms-api-gateway/
│   └── src/main/java/.../apigateway/
│       ├── config/             # Routes, CORS, Rate Limiting (Redis)
│       └── filter/             # JwtAuthGatewayFilter (global)
│
├── ms-user/
│   └── src/main/java/.../userservice/
│       ├── config/             # Redis, Security, Kafka, Swagger
│       ├── controller/         # Auth, User, InternalUser controllers
│       ├── entity/             # User, Session, RefreshToken
│       ├── security/           # JWT filter, CustomUserDetailsService
│       └── service/            # Auth, JWT, RedisDenylistService
│
├── ms-product/
│   └── src/main/
│       ├── java/.../productservice/
│       │   ├── config/         # Redis, MinIO, Security, Kafka
│       │   ├── controller/     # Product, Category controllers
│       │   ├── entity/         # Product (JSONB), Category (hierarchical), ProductImage, OutboxEvent
│       │   ├── grpc/           # gRPC server + exception advice
│       │   ├── kafka/          # OutboxPublisher, DlqService, consumers
│       │   └── service/        # Business logic, MinIO service
│       └── proto/              # product.proto
│
├── ms-inventory/
│   └── src/main/
│       ├── java/.../inventoryservice/
│       │   ├── entity/         # Inventory, StockReservation, OutboxEvent
│       │   ├── grpc/           # gRPC server + exception advice
│       │   ├── kafka/          # OutboxPublisher, DlqService, consumers
│       │   └── service/        # Stock management, reservation logic
│       └── proto/              # inventory.proto
│
├── ms-cart/
│   └── src/main/java/.../cartservice/
│       ├── entity/             # Cart, CartItem (unique per user)
│       ├── grpc/               # gRPC clients (product, inventory)
│       ├── kafka/              # DlqService, payment/product/inventory consumers
│       └── service/            # Cart logic with gRPC stock validation
│
├── ms-order/
│   └── src/main/java/.../orderservice/
│       ├── client/             # CartServiceClient (REST)
│       ├── entity/             # Order, OrderItem, OrderStatusHistory, OutboxEvent, ProcessedEvent
│       ├── grpc/               # gRPC clients (product, inventory)
│       ├── kafka/              # OutboxPublisher, DlqService, idempotent consumers
│       └── service/            # Order lifecycle, stock reservation
│
├── ms-payment/
│   └── src/main/java/.../paymentservice/
│       ├── client/             # KapitalBankClient (JSON/BasicAuth), OrderServiceClient
│       ├── controller/         # Payment + callback controllers
│       ├── entity/             # Payment, PaymentStatusHistory, OutboxEvent
│       ├── kafka/              # OutboxPublisher, DlqService, consumers
│       └── service/            # Payment flow, refund logic, test mode
│
├── ms-notification/
│   └── src/main/java/.../notificationservice/
│       ├── client/             # UserServiceClient (REST, cached)
│       ├── controller/         # Notification REST + SSE stream
│       ├── entity/             # Notification (with retry tracking)
│       ├── kafka/              # DlqService, order/payment/inventory consumers
│       └── service/            # NotificationService, EmailService, SseEmitterService, RetryScheduler
│
└── frontend/
    ├── src/
    │   ├── api/                # Axios client, auth interceptor, per-domain API files
    │   ├── components/         # Layout, Product, Cart, Order, UI, Admin, Category
    │   ├── pages/              # Home, Products, Cart, Checkout, Orders, Admin pages
    │   ├── hooks/              # useAuth, useCart, useOrders, usePayments, useProducts, useSSE
    │   ├── context/            # AuthContext
    │   ├── providers/          # QueryProvider (TanStack React Query)
    │   └── utils/              # Validators, formatters, constants
    ├── package.json
    ├── vite.config.js
    └── Dockerfile              # Node build + Nginx serve
```

## Author

Mahammad - [GitHub](https://github.com/mstfzade22)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
