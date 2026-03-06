# E-Commerce Microservices Application

A cloud-native e-commerce platform built with microservices architecture using Spring Boot 4.x, gRPC, Apache Kafka, Redis, PostgreSQL, and Kapital Bank payment integration.

## Architecture Overview

```
                              ┌──────────────┐
                              │   Frontend   │
                              └──────┬───────┘
                                     │
         ┌───────────┬───────────┬───┴────┬───────────┬──────────────┐
         ▼           ▼           ▼        ▼           ▼              ▼
  ┌─────────────┐┌──────────┐┌────────┐┌───────┐┌──────────┐┌────────────┐
  │user-service ││ product  ││  cart  ││ order ││ payment  ││ inventory  │
  │   :8081     ││ :8082    ││ :8084  ││ :8085 ││  :8086   ││   :8083    │
  │             ││ gRPC:9090││        ││       ││          ││ gRPC:9091  │
  └──────┬──────┘└────┬─────┘└───┬────┘└───┬───┘└────┬─────┘└─────┬─────┘
         │            │          │         │         │             │
         │       ┌────┴──────────┴─────────┴─────────┴─────────────┘
         │       │                  Apache Kafka
         │       │    product-events │ inventory-events │ order-events │ payment-events
         │       └──────────────────────────────────────────────────────
         │
    ┌────┴────┐  ┌─────────┐  ┌───────┐  ┌───────┐  ┌─────────────┐
    │  Vault  │  │PostgreSQL│  │ Redis │  │ MinIO │  │ Kapital Bank│
    │ :8200   │  │  :5432   │  │ :6379 │  │ :9000 │  │  (external) │
    └─────────┘  └─────────┘  └───────┘  └───────┘  └─────────────┘
```

## Services

| Service | Port | Description |
|:--------|:-----|:------------|
| **user-service** | 8081 | Authentication, registration, JWT token management, session tracking |
| **product-service** | 8082, 9090 (gRPC) | Product & category CRUD, image upload via MinIO, gRPC server |
| **inventory-service** | 8083, 9091 (gRPC) | Stock management, reservations, gRPC server |
| **cart-service** | 8084 | Shopping cart with real-time stock validation via gRPC |
| **order-service** | 8085 | Order lifecycle, stock reservation via gRPC, cart integration via REST |
| **payment-service** | 8086 | Kapital Bank payment gateway integration (JSON REST API) |

## Technology Stack

| Category | Technology |
|:---------|:-----------|
| **Framework** | Spring Boot 4.x, Spring Security, Spring Data JPA |
| **Languages** | Java 21 |
| **API** | REST, gRPC (inter-service), Protocol Buffers |
| **Database** | PostgreSQL 16 (per-service databases, JSONB columns) |
| **Caching** | Redis 7 (response caching, token denylist) |
| **Messaging** | Apache Kafka 3.7 (KRaft mode, event-driven architecture) |
| **Payment** | Kapital Bank E-Commerce API (BasicAuth, JSON) |
| **Object Storage** | MinIO (S3-compatible, product images) |
| **Secrets** | HashiCorp Vault (KV v2, per-service secrets) |
| **Mapping** | MapStruct (DTO/entity conversion) |
| **Documentation** | OpenAPI 3.0 / Swagger UI |
| **Build** | Gradle 9.3 |
| **Containerization** | Docker & Docker Compose |

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose

### 1. Clone and Start Infrastructure

```bash
git clone https://github.com/mstfzade22/E-commerce-Microservices-App.git
cd E-commerce-Microservices-App
docker compose up -d
```

This starts PostgreSQL, Redis, Kafka, Vault (with auto-init), MinIO, Redpanda Console, and Redis Insight.

### 2. Run Services

```bash
# Each in a separate terminal
cd user-service && ./gradlew bootRun
cd product-service && ./gradlew bootRun
cd inventory-service && ./gradlew bootRun
cd cart-service && ./gradlew bootRun
cd order-service && ./gradlew bootRun
cd payment-service && ./gradlew bootRun
```

> The terminal may stay at 80-87% EXECUTING — this is normal for Spring Boot. Check logs for `Started ... Application`.

### 3. Create MinIO Bucket (first run only)

```bash
# Via MinIO Console at http://localhost:9001 (minioadmin/minioadmin)
# Create bucket: product-images

# Or via CLI
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/product-images
mc anonymous set download local/product-images
```

## API Endpoints

### User Service (Port 8081)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/auth/register` | POST | Public | Register new user |
| `/api/v1/auth/login` | POST | Public | Login, returns JWT in HttpOnly cookies |
| `/api/v1/auth/refresh` | POST | Public | Refresh access token |
| `/api/v1/auth/logout` | POST | Auth | Revoke tokens, add to denylist |
| `/api/v1/auth/validate` | GET | Auth | Validate current token |
| `/api/v1/users/me` | GET | Auth | Current user profile |
| `/api/v1/users/{id}` | GET | Auth | Get user by ID |
| `/api/v1/users/me` | PUT | Auth | Update profile |
| `/api/v1/users/{id}/lock` | POST | ADMIN | Lock user account |
| `/api/v1/users/{id}/unlock` | POST | ADMIN | Unlock user account |

### Product Service (Port 8082)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/products` | GET | Public | List products (paginated) |
| `/api/v1/products/{id}` | GET | Public | Get product by ID |
| `/api/v1/products/slug/{slug}` | GET | Public | Get product by slug |
| `/api/v1/products/search?keyword=` | GET | Public | Search products |
| `/api/v1/products/featured` | GET | Public | Featured products |
| `/api/v1/products/category/{id}` | GET | Public | Products by category |
| `/api/v1/products` | POST | ADMIN/STORE | Create product (auto-creates inventory) |
| `/api/v1/products/{id}` | PUT | ADMIN/STORE | Update product |
| `/api/v1/products/{id}` | DELETE | ADMIN/STORE | Soft delete product |
| `/api/v1/products/{id}/images` | POST | ADMIN/STORE | Upload image to MinIO |
| `/api/v1/categories` | GET | Public | List root categories |
| `/api/v1/categories/tree` | GET | Public | Full category hierarchy |
| `/api/v1/categories` | POST | ADMIN | Create category |

### Inventory Service (Port 8083)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/inventory/{productId}` | GET | Public | Get stock info |
| `/api/v1/inventory/{productId}/status` | GET | Public | Simplified stock status |
| `/api/v1/inventory/{productId}/stock` | PUT | ADMIN | Update stock quantity |
| `/api/v1/inventory/{productId}/reserve` | POST | ADMIN/STORE | Reserve stock |
| `/api/v1/inventory/{productId}/release` | POST | ADMIN | Release reservation |
| `/api/v1/inventory/low-stock` | GET | ADMIN | Low stock products |

### Cart Service (Port 8084)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/cart` | GET | Auth | Get cart |
| `/api/v1/cart/items` | POST | Auth | Add item to cart |
| `/api/v1/cart/items/{productId}` | PUT | Auth | Update quantity |
| `/api/v1/cart/items/{productId}` | DELETE | Auth | Remove item |
| `/api/v1/cart` | DELETE | Auth | Clear cart |
| `/api/v1/cart/validate` | POST | Auth | Validate cart before checkout |
| `/api/v1/cart/summary` | GET | Auth | Cart summary |

### Order Service (Port 8085)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/orders` | POST | Auth | Create order from cart |
| `/api/v1/orders` | GET | Auth | Get user's orders (paginated) |
| `/api/v1/orders/{orderNumber}` | GET | Auth | Get order details |
| `/api/v1/orders/{orderNumber}/confirm` | POST | Auth | Confirm order |
| `/api/v1/orders/{orderNumber}/cancel` | POST | Auth | Cancel order |
| `/api/v1/orders/{orderNumber}/process` | POST | STORE/ADMIN | Mark processing |
| `/api/v1/orders/{orderNumber}/ship` | POST | STORE/ADMIN | Mark shipped |
| `/api/v1/orders/{orderNumber}/deliver` | POST | STORE/ADMIN | Mark delivered |
| `/api/v1/orders/{orderNumber}/history` | GET | Auth | Status change history |

### Payment Service (Port 8086)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/payments/initiate?amount=` | POST | Auth | Initiate payment, returns Kapital Bank URL |
| `/api/v1/payments/{id}` | GET | Auth | Get payment details |
| `/api/v1/payments/order/{orderNumber}` | GET | Auth | Get payment by order |
| `/api/v1/payments/my` | GET | Auth | User's payments (paginated) |
| `/api/v1/payments/{id}/history` | GET | Auth | Payment status history |
| `/api/v1/payments/{id}/refund` | POST | ADMIN | Refund payment |
| `/api/v1/payments/callback/result` | GET | Public | Kapital Bank callback |

### gRPC Services

| Service | Port | Methods |
|:--------|:-----|:--------|
| **Product gRPC** | 9090 | GetProduct, GetProductsByIds, CheckProductExists, GetProductStockStatus, GetProductsByCategory |
| **Inventory gRPC** | 9091 | CheckStock, ReserveStock, ReleaseStock, ConfirmStock, GetStockInfo |

## Kafka Topics & Events

| Topic | Producer | Consumer | Events |
|:------|:---------|:---------|:-------|
| `product-events` | product-service | inventory-service | ProductCreated, ProductUpdated, ProductDeleted, PriceChanged |
| `inventory-events` | inventory-service | product-service | StockUpdated, StockReserved, StockReleased, StockConfirmed |
| `order-events` | order-service | payment-service | OrderCreated, OrderConfirmed, OrderCancelled, OrderShipped, OrderDelivered |
| `payment-events` | payment-service | order-service | PaymentInitiated, PaymentSuccess, PaymentFailed, PaymentRefunded |

## Database Schema

Each service has its own PostgreSQL database:

| Database | Tables |
|:---------|:-------|
| `user_service_db` | users, sessions, refresh_tokens |
| `product_service_db` | products, categories, product_images |
| `inventory_service_db` | inventory, stock_reservations |
| `cart_service_db` | carts, cart_items |
| `order_service_db` | orders, order_items, order_status_history |
| `payment_service_db` | payments, payment_status_history |

## Security

- **Authentication:** JWT (HS512) with access/refresh token pair
- **Token Delivery:** HttpOnly cookies (`access_token`, `refresh_token`)
- **Token Rotation:** Refresh tokens are rotated on use, old tokens are revoked
- **Token Revocation:** Redis-based session denylist with TTL
- **Secret Management:** HashiCorp Vault (KV v2, per-service paths)
- **Password Hashing:** BCrypt
- **Roles:** CUSTOMER, STORE, ADMIN

## Docker Infrastructure

| Service | Image | Ports | UI |
|:--------|:------|:------|:---|
| PostgreSQL | `postgres:16` | 5432 | - |
| Redis | `redis:7-alpine` | 6379 | - |
| Kafka (KRaft) | `apache/kafka:3.7.0` | 9092, 29092 | - |
| Vault | `hashicorp/vault:1.15` | 8200 | http://localhost:8200 |
| MinIO | `minio/minio:latest` | 9000, 9001 | http://localhost:9001 |
| Redpanda Console | `redpandadata/console` | 8080 | http://localhost:8080 |
| Redis Insight | `redis/redisinsight` | 5540 | http://localhost:5540 |

## API Documentation (Swagger)

| Service | URL |
|:--------|:----|
| User Service | http://localhost:8081/api/v1/swagger-ui.html |
| Product Service | http://localhost:8082/api/v1/swagger-ui.html |
| Inventory Service | http://localhost:8083/api/v1/swagger-ui.html |
| Cart Service | http://localhost:8084/api/v1/swagger-ui.html |
| Order Service | http://localhost:8085/api/v1/swagger-ui.html |
| Payment Service | http://localhost:8086/api/v1/swagger-ui.html |

## Monitoring

| Tool | URL | Purpose |
|:-----|:----|:--------|
| Actuator Health | `/api/v1/actuator/health` on each service | Health checks |
| Redpanda Console | http://localhost:8080 | Kafka topics & messages |
| Redis Insight | http://localhost:5540 | Redis data browser |
| MinIO Console | http://localhost:9001 | Object storage browser |
| Vault UI | http://localhost:8200 | Secrets management |

## Project Structure

```
E-commerce-Microservices-App/
├── docker-compose.yml
├── init-databases.sql
├── README.md
├── ARCHITECTURE.md
│
├── user-service/
│   └── src/main/java/com/ecommerce/userservice/
│       ├── config/          # Redis, Security, Kafka, Swagger
│       ├── controller/      # Auth, User REST controllers
│       ├── dto/             # Request/Response DTOs
│       ├── entity/          # User, Session, RefreshToken
│       ├── exception/       # Custom exceptions + global handler
│       ├── mapper/          # MapStruct mappers
│       ├── repository/      # Spring Data JPA repos
│       ├── security/        # JWT filter
│       └── service/         # Business logic, JWT util, Redis denylist
│
├── product-service/
│   └── src/main/
│       ├── java/.../productservice/
│       │   ├── config/      # Redis, MinIO, Security, Kafka
│       │   ├── controller/  # Product, Category REST controllers
│       │   ├── entity/      # Product (JSONB attrs), Category (self-ref), ProductImages
│       │   ├── grpc/        # gRPC server implementation
│       │   ├── kafka/       # Producers & consumers
│       │   ├── mapper/      # MapStruct mappers
│       │   ├── repositories/
│       │   └── service/     # Business logic, MinIO service
│       └── proto/           # product.proto
│
├── inventory-service/
│   └── src/main/
│       ├── java/.../inventoryservice/
│       │   ├── entity/      # Inventory, StockReservation
│       │   ├── grpc/        # gRPC server implementation
│       │   ├── kafka/       # Producers & consumers
│       │   └── service/     # Stock management, reservation logic
│       └── proto/           # inventory.proto
│
├── cart-service/
│   └── src/main/java/.../cartservice/
│       ├── entity/          # Cart, CartItem (unique per user)
│       ├── grpc/            # gRPC clients (product, inventory)
│       └── service/         # Cart logic with stock validation
│
├── order-service/
│   └── src/main/java/.../orderservice/
│       ├── client/          # CartServiceClient (REST)
│       ├── entity/          # Order, OrderItem, OrderStatusHistory
│       ├── grpc/            # gRPC clients (product, inventory)
│       ├── kafka/           # Producers & consumers
│       └── service/         # Order lifecycle, stock reservation
│
└── payment-service/
    └── src/main/java/.../paymentservice/
        ├── client/          # KapitalBankClient (JSON/BasicAuth), OrderServiceClient
        ├── controller/      # Payment + callback controllers
        ├── entity/          # Payment, PaymentStatusHistory
        ├── kafka/           # Producers & consumers
        └── service/         # Payment flow, refund logic
```

## Author

Mahammad - [GitHub](https://github.com/mstfzade22)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
