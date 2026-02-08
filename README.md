# E-Commerce Microservices Application

A modern, cloud-native e-commerce platform built with microservices architecture using Spring Boot 4.x, gRPC, Apache Kafka, Redis, and PostgreSQL.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND                                        │
│                           (React, Vue, etc.)                                │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API GATEWAY (:8080)                                │
│                      (Routing, JWT, Rate Limiting)                          │
└─────────────────────────────────────────────────────────────────────────────┘
          │              │              │              │              │
          ▼              ▼              ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ user-service │ │product-service│ │ cart-service │ │order-service │ │payment-service│
│   (:8081)    │ │   (:8082)    │ │   (:8084)    │ │   (:8085)    │ │   (:8086)    │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
        │               │                                  │              
        ▼               ▼                                  ▼              
┌──────────────────────────────────────────────────────────────────────────────┐
│                              KAFKA (Events)                                  │
│                     product-events, inventory-events                         │
└──────────────────────────────────────────────────────────────────────────────┘
```

## 📦 Services

| Service | Port | Description |
|:--------|:-----|:------------|
| **user-service** | 8081 | Authentication, registration, JWT tokens |
| **product-service** | 8082, 9090 (gRPC) | Product & category management, images |
| **api-gateway** | 8080 | Request routing, rate limiting |
| **inventory-service** | 8083 | Stock management |
| **cart-service** | 8084 | Shopping cart |
| **order-service** | 8085 | Order management |
| **payment-service** | 8086 | Payment processing |
| **notification-service** | 8087 | Email/SMS notifications |

## 🛠️ Technology Stack

| Category | Technology |
|:---------|:-----------|
| **Framework** | Spring Boot 4.x, Spring Security, Spring Data JPA |
| **API** | REST, gRPC (inter-service communication) |
| **Database** | PostgreSQL 16 |
| **Caching** | Redis 7 |
| **Messaging** | Apache Kafka (KRaft mode) |
| **Object Storage** | MinIO (S3-compatible) |
| **Secrets Management** | HashiCorp Vault |
| **Documentation** | OpenAPI 3.0 / Swagger UI |
| **Build Tool** | Gradle 9.3 |
| **Java Version** | Java 21 |

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Gradle 9.3 (or use the included wrapper)

### 1. Clone the Repository

```bash
git clone https://github.com/mstfzade22/E-commerce-Microservices-App.git
cd E-commerce-Microservices-App
```

### 2. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka with KRaft (port 9092, 29092)
- Vault (port 8200)
- MinIO (ports 9000, 9001)
- Redpanda Console - Kafka UI (port 8080)
- Redis Insight (port 5540)

### 3. Inject Vault Secrets

> **Note:** The secrets provided below are for **local development only**. In a production environment, use unique, high-entropy keys and never commit real secrets to version control.

```bash
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='root-token'

# User Service secrets
vault kv put secret/user-service \
  spring.datasource.url="jdbc:postgresql://localhost:5432/user_service_db" \
  spring.datasource.username="postgres" \
  spring.datasource.password="postgres" \
  spring.data.redis.host="localhost" \
  spring.data.redis.port="6379" \
  spring.data.redis.password="" \
  jwt.access.secret="your-super-secret-access-key-at-least-32-characters-long" \
  jwt.refresh.secret="your-super-secret-refresh-key-at-least-32-characters-long" \
  jwt.access.expiration="3600" \
  jwt.refresh.expiration="604800"

# Product Service secrets
vault kv put secret/product-service \
  spring.datasource.url="jdbc:postgresql://localhost:5432/product_service_db" \
  spring.datasource.username="postgres" \
  spring.datasource.password="postgres" \
  spring.data.redis.host="localhost" \
  spring.data.redis.port="6379" \
  spring.data.redis.password="" \
  minio.access-key="minioadmin" \
  minio.secret-key="minioadmin"
```

### 4. Create Databases

```bash
docker exec -it postgres psql -U postgres -c "CREATE DATABASE user_service_db;"
docker exec -it postgres psql -U postgres -c "CREATE DATABASE product_service_db;"
```

### 5. Create MinIO Bucket

```bash
# Using MinIO Console
# Go to http://localhost:9001, login with minioadmin/minioadmin
# Create bucket: product-images

# Or using mc CLI
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/product-images
mc anonymous set download local/product-images
```

### 6. Run Services

```bash
# Terminal 1 - User Service
cd user-service && ./gradlew bootRun

# Terminal 2 - Product Service
cd product-service && ./gradlew bootRun
```

> **Note:** When running `./gradlew bootRun`, the terminal may stay at **80-87% EXECUTING**. This is normal behavior for Spring Boot applications as the Gradle process remains active to serve requests. Check the logs for `Started ... Application` to confirm the service is ready.

## 📚 API Documentation

| Service | Swagger UI |
|:--------|:-----------|
| User Service | http://localhost:8081/api/v1/swagger-ui.html |
| Product Service | http://localhost:8082/api/v1/swagger-ui.html |

## 🔌 Service Endpoints

### User Service (Port 8081)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/auth/register` | POST | Public | Register new user |
| `/api/v1/auth/login` | POST | Public | Login, get JWT tokens |
| `/api/v1/auth/refresh` | POST | Public | Refresh access token |

### Product Service (Port 8082)

| Endpoint | Method | Auth | Description |
|:---------|:-------|:-----|:------------|
| `/api/v1/products` | GET | Public | List all products |
| `/api/v1/products/{id}` | GET | Public | Get product by ID |
| `/api/v1/products/slug/{slug}` | GET | Public | Get product by slug |
| `/api/v1/products/search` | GET | Public | Search products |
| `/api/v1/products/featured` | GET | Public | Get featured products |
| `/api/v1/products/category/{id}` | GET | Public | Get products by category |
| `/api/v1/products` | POST | ADMIN/STORE | Create product |
| `/api/v1/products/{id}` | PUT | ADMIN/STORE | Update product |
| `/api/v1/products/{id}` | DELETE | ADMIN/STORE | Delete product |
| `/api/v1/categories` | GET | Public | List root categories |
| `/api/v1/categories/tree` | GET | Public | Get category tree |
| `/api/v1/categories/{id}` | GET | Public | Get category by ID |
| `/api/v1/categories` | POST | ADMIN | Create category |
| `/api/v1/categories/{id}` | PUT | ADMIN | Update category |
| `/api/v1/categories/{id}` | DELETE | ADMIN | Delete category |
| `/api/v1/products/{id}/images` | GET | Public | Get product images |
| `/api/v1/products/{id}/images` | POST | ADMIN/STORE | Upload image |
| `/api/v1/products/{id}/images/{imgId}` | DELETE | ADMIN/STORE | Delete image |

### Product Service gRPC (Port 9090)

| Method | Description |
|:-------|:------------|
| `GetProduct` | Get product by ID |
| `GetProductsByIds` | Bulk fetch products |
| `CheckProductExists` | Check if product exists |
| `GetProductStockStatus` | Get stock status |
| `GetProductsByCategory` | Get products by category |

## 📨 Kafka Topics

| Topic | Producer | Consumer | Events |
|:------|:---------|:---------|:-------|
| `product-events` | product-service | order, cart, inventory | ProductCreated, ProductUpdated, ProductDeleted, PriceChanged |
| `inventory-events` | inventory-service | product-service | StockUpdated |

## 🗄️ Database Schema

### User Service Database (`user_service_db`)

- `users` - User accounts
- `sessions` - Active user sessions

### Product Service Database (`product_service_db`)

- `products` - Product catalog
- `categories` - Product categories (hierarchical)
- `product_images` - Product images

## 🔐 Security

- **Authentication:** JWT-based stateless authentication
- **Token Storage:** HTTP-only cookies (access token)
- **Secret Management:** HashiCorp Vault
- **Password Hashing:** BCrypt

### JWT Token Flow

```
┌──────────┐     ┌─────────────────┐
│  Client  │────►│  user-service   │ :8081
└──────────┘     │  (issues JWT)   │
      │          └─────────────────┘
      │                    │
      │                    ▼
      │          ┌─────────────────┐
      │          │     Vault       │ (shared secret)
      │          └─────────────────┘
      │                    │
      │                    ▼
      └─────────►│ product-service │ :8082
                 │ (validates JWT) │
                 └─────────────────┘
```

## 🐳 Docker Services

| Service | Image | Ports | UI |
|:--------|:------|:------|:---|
| PostgreSQL | `postgres:16` | 5432 | - |
| Redis | `redis:7-alpine` | 6379 | - |
| Kafka (KRaft) | `apache/kafka:3.7.0` | 9092, 29092 | - |
| Vault | `hashicorp/vault:1.15` | 8200 | http://localhost:8200 |
| MinIO | `minio/minio:latest` | 9000, 9001 | http://localhost:9001 |
| Redpanda Console | `redpandadata/console:latest` | 8080 | http://localhost:8080 |
| Redis Insight | `redis/redisinsight:latest` | 5540 | http://localhost:5540 |

## 📁 Project Structure

```
E-commerce-Microservices-App/
├── docker-compose.yml          # Infrastructure services
├── init-databases.sql          # Database initialization
├── INFRASTRUCTURE.md           # Infrastructure setup guide
├── README.md                   # This file
│
├── user-service/               # Authentication & User Management
│   ├── src/main/java/...
│   ├── src/main/resources/
│   │   └── application.yml
│   └── build.gradle
│
└── product-service/            # Product & Category Management
    ├── src/main/java/com/ecommerce/productservice/
    │   ├── config/             # Configuration classes
    │   ├── controller/         # REST controllers
    │   ├── dto/                # Request/Response DTOs
    │   ├── entity/             # JPA entities
    │   ├── exception/          # Custom exceptions
    │   ├── grpc/               # gRPC service implementation
    │   ├── kafka/              # Kafka consumers
    │   ├── mapper/             # MapStruct mappers
    │   ├── repositories/       # Spring Data JPA repositories
    │   ├── security/           # JWT authentication
    │   └── service/            # Business logic
    ├── src/main/proto/         # Protocol Buffer definitions
    ├── src/main/resources/
    │   └── application.yml
    └── build.gradle
```

## 🧪 Testing

```bash
# Run tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## 📊 Monitoring

| Tool | URL | Purpose |
|:-----|:----|:--------|
| Actuator Health | `/actuator/health` | Service health check |
| Redpanda Console | http://localhost:8080 | Kafka topics & messages |
| Redis Insight | http://localhost:5540 | Redis data browser |
| MinIO Console | http://localhost:9001 | Object storage browser |
| Vault UI | http://localhost:8200 | Secrets management |

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

Mahammad - [GitHub](https://github.com/mstfzade22)

---

**Happy Coding! 🚀**
