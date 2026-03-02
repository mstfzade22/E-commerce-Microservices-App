# E-Commerce Microservices - Architecture & Technology Scenarios

## Table of Contents

- [Technology Overview](#technology-overview)
- [Service Communication Map](#service-communication-map)
- [Kafka Event Flows](#kafka-event-flows)
- [Redis Caching Strategies](#redis-caching-strategies)
- [MinIO Object Storage](#minio-object-storage)
- [gRPC Communication](#grpc-communication)
- [End-to-End Scenarios](#end-to-end-scenarios)

---

## Technology Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                            │
│                                                                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │ PostgreSQL │  │   Redis    │  │   Kafka    │  │   MinIO    │       │
│  │   :5432    │  │   :6379    │  │   :9092    │  │   :9000    │       │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘       │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                          SERVICE LAYER                                  │
│                                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │ User Service │  │Product Serv. │  │Inventory Srv │  │Cart Service│ │
│  │  REST :8081  │  │ REST  :8082  │  │ REST  :8083  │  │ REST :8084 │ │
│  │              │  │ gRPC  :9090  │  │ gRPC  :9091  │  │ gRPC client│ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

### Technology Usage Per Service

| Technology       | User Service | Product Service   | Inventory Service | Cart Service       |
| ---------------- | ------------ | ----------------- | ----------------- | ------------------ |
| **PostgreSQL**   | Users, Sessions, Tokens | Products, Categories, Images | Inventory, Reservations | Carts, CartItems |
| **Redis**        | Token denylist (30min) | Product cache (1h), Category cache (6h) | Stock cache (5min) | Cart cache (30min) |
| **Kafka Producer** | --         | `product-events`  | `inventory-events` | `cart-events`     |
| **Kafka Consumer** | --         | `inventory-events`| `product-events`   | `product-events`, `inventory-events` |
| **MinIO**        | --           | Product images    | --                | --                 |
| **gRPC Server**  | --           | 5 RPCs on :9090   | 5 RPCs on :9091   | --                 |
| **gRPC Client**  | --           | --                | --                | Calls Product + Inventory |
| **JWT**          | Issues tokens | Validates tokens | Validates tokens  | Validates tokens   |

---

## Service Communication Map

```
                          ┌─────────────────┐
                          │   User Service   │
                          │     :8081        │
                          │                  │
                          │  Issues JWT      │
                          └────────┬─────────┘
                                   │ JWT tokens validated by all services
                    ┌──────────────┼──────────────────┐
                    ▼              ▼                   ▼
          ┌─────────────┐  ┌──────────────┐  ┌────────────────┐
          │   Product    │  │  Inventory   │  │  Cart Service  │
          │   Service    │  │  Service     │  │    :8084       │
          │  :8082/:9090 │  │ :8083/:9091  │  │                │
          └──────┬───────┘  └──────┬───────┘  └───┬───────┬────┘
                 │                 │               │       │
                 │    Kafka        │    Kafka      │gRPC   │gRPC
                 │◄────────────────┤               │       │
                 ├────────────────►│               │       │
                 ├─────────────────┼──────────────►│       │
                 │                 ├──────────────►│       │
                 │◄────────────────┼───────────────┤       │
                 │                 │◄──────────────┼───────┘
                 │                 │               │
```

### Communication Protocols

| From            | To              | Protocol | Purpose                          |
| --------------- | --------------- | -------- | -------------------------------- |
| Cart Service    | Product Service | **gRPC** | Fetch product details, validate existence |
| Cart Service    | Inventory Serv. | **gRPC** | Check stock availability         |
| Product Service | Inventory Serv. | **Kafka**| ProductCreated/Deleted events    |
| Inventory Serv. | Product Service | **Kafka**| StockUpdated event (sync status) |
| Product Service | Cart Service    | **Kafka**| PriceChanged, ProductDeleted     |
| Inventory Serv. | Cart Service    | **Kafka**| StockUpdated event               |

---

## Kafka Event Flows

### Topics & Events

```
┌──────────────────────────────────────────────────────────────────────┐
│                        KAFKA BROKER                                 │
│                                                                     │
│  Topic: product-events                                              │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  ProductCreatedEvent  │  ProductUpdatedEvent                  │  │
│  │  ProductDeletedEvent  │  PriceChangedEvent                    │  │
│  └────────────────────────────────────────────────────────────────┘  │
│  Produced by: Product Service                                       │
│  Consumed by: Inventory Service, Cart Service                       │
│                                                                     │
│  Topic: inventory-events                                            │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  StockUpdatedEvent    │  StockReservedEvent                   │  │
│  │  StockReleasedEvent   │  StockConfirmedEvent                  │  │
│  └────────────────────────────────────────────────────────────────┘  │
│  Produced by: Inventory Service                                     │
│  Consumed by: Product Service, Cart Service                         │
│                                                                     │
│  Topic: cart-events                                                 │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  CartItemAddedEvent   │  CartItemUpdatedEvent                 │  │
│  │  CartItemRemovedEvent │  CartClearedEvent                     │  │
│  │  CartConvertedEvent   │                                       │  │
│  └────────────────────────────────────────────────────────────────┘  │
│  Produced by: Cart Service                                          │
│  Consumed by: (Order Service - future)                              │
│                                                                     │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Scenario 1: Admin Creates a New Product

#### SUCCESS Flow

```
 Admin                Product Service           Kafka              Inventory Service
  │                        │                      │                       │
  │  POST /products        │                      │                       │
  │  {name, price,         │                      │                       │
  │   initialStock: 100}   │                      │                       │
  ├───────────────────────►│                      │                       │
  │                        │                      │                       │
  │                        │  Save to PostgreSQL  │                       │
  │                        │  (product record)    │                       │
  │                        │──────────┐           │                       │
  │                        │◄─────────┘           │                       │
  │                        │                      │                       │
  │                        │  Evict Redis cache   │                       │
  │                        │──────────┐           │                       │
  │                        │◄─────────┘           │                       │
  │                        │                      │                       │
  │                        │  ProductCreatedEvent │                       │
  │                        │  {productId,         │                       │
  │                        │   initialStock: 100} │                       │
  │                        ├─────────────────────►│                       │
  │                        │                      │   Consume event       │
  │                        │                      ├──────────────────────►│
  │                        │                      │                       │
  │  201 Created           │                      │   Create inventory:   │
  │◄───────────────────────┤                      │   quantity=100        │
  │                        │                      │   reservedQty=0       │
  │                        │                      │   status=AVAILABLE    │
  │                        │                      │   ┌──────────┐        │
  │                        │                      │   │PostgreSQL│        │
  │                        │                      │   └──────────┘        │
  │                        │                      │                       │
  │                        │                      │   StockUpdatedEvent   │
  │                        │                      │◄──────────────────────┤
  │                        │  Consume event       │                       │
  │                        │◄─────────────────────┤                       │
  │                        │                      │                       │
  │                        │  Update stockStatus  │                       │
  │                        │  = AVAILABLE         │                       │
  │                        │──────────┐           │                       │
  │                        │◄─────────┘           │                       │
```

#### FAILURE Flow - Kafka Delivery Fails

```
 Admin                Product Service           Kafka              Inventory Service
  │                        │                      │                       │
  │  POST /products        │                      │                       │
  ├───────────────────────►│                      │                       │
  │                        │                      │                       │
  │                        │  Save to PostgreSQL  │                       │
  │                        │──────────┐  OK       │                       │
  │                        │◄─────────┘           │                       │
  │                        │                      │                       │
  │                        │  ProductCreatedEvent │                       │
  │                        ├──────────────────────X  Kafka broker down!   │
  │                        │                      │                       │
  │  201 Created           │                      │                       │
  │◄───────────────────────┤                      │                       │
  │                        │                      │                       │
  │    Product exists in DB but NO inventory      │                       │
  │    record is created. Stock = unknown.        │                       │
  │                                               │                       │
  │    CONSEQUENCE:                               │                       │
  │    - Product shows in catalog                 │                       │
  │    - Cart service gRPC stock check fails      │                       │
  │    - Users cannot add product to cart          │                       │
  │                                               │                       │
  │    RESOLUTION:                                │                       │
  │    - Admin manually creates inventory via     │                       │
  │      POST /inventory endpoint                 │                       │
  │    - Or: Kafka retries deliver the event      │                       │
```

---

### Scenario 2: Price Change Propagation

#### SUCCESS Flow

```
 Admin           Product Service         Kafka           Cart Service         Inventory
  │                    │                   │                   │                  │
  │ PUT /products/{id} │                   │                   │                  │
  │ {price: 29.99}     │                   │                   │                  │
  ├───────────────────►│                   │                   │                  │
  │                    │                   │                   │                  │
  │                    │ Update PostgreSQL │                   │                  │
  │                    │─────────┐         │                   │                  │
  │                    │◄────────┘         │                   │                  │
  │                    │                   │                   │                  │
  │                    │ Evict Redis cache │                   │                  │
  │                    │─────────┐         │                   │                  │
  │                    │◄────────┘         │                   │                  │
  │                    │                   │                   │                  │
  │                    │ PriceChangedEvent │                   │                  │
  │                    │ {productId,       │                   │                  │
  │                    │  oldPrice: 39.99, │                   │                  │
  │                    │  newPrice: 29.99} │                   │                  │
  │                    ├──────────────────►│                   │                  │
  │                    │                   │                   │                  │
  │ 200 OK             │                   │  Consume event    │                  │
  │◄───────────────────┤                   ├──────────────────►│                  │
  │                    │                   │                   │                  │
  │                    │                   │   Find all carts  │                  │
  │                    │                   │   containing      │                  │
  │                    │                   │   this productId  │                  │
  │                    │                   │   ┌──────────┐    │                  │
  │                    │                   │   │PostgreSQL│    │                  │
  │                    │                   │   └──────────┘    │                  │
  │                    │                   │                   │                  │
  │                    │                   │   Update unitPrice│                  │
  │                    │                   │   in all matching │                  │
  │                    │                   │   cart items       │                  │
  │                    │                   │   ┌──────────┐    │                  │
  │                    │                   │   │PostgreSQL│    │                  │
  │                    │                   │   └──────────┘    │                  │
  │                    │                   │                   │                  │
  │                    │                   │   Evict cart cache │                  │
  │                    │                   │   from Redis      │                  │
  │                    │                   │   ┌──────┐        │                  │
  │                    │                   │   │Redis │        │                  │
  │                    │                   │   └──────┘        │                  │
  │                    │                   │                   │                  │
  │    All active carts now reflect the new price             │                  │
```

#### FAILURE Flow - Cart Service Down

```
 Admin           Product Service         Kafka           Cart Service
  │                    │                   │                   │
  │ PUT /products/{id} │                   │                   │
  │ {price: 29.99}     │                   │                   │
  ├───────────────────►│                   │                   │
  │                    │ ... processes ... │                   │
  │ 200 OK             │                   │                   │
  │◄───────────────────┤                   │                   │
  │                    │ PriceChangedEvent │                   │
  │                    ├──────────────────►│                   │
  │                    │                   │  Cart Service     │
  │                    │                   │  is DOWN!         │
  │                    │                   │       X           │
  │                    │                   │                   │
  │    Kafka retains the event (consumer offset not committed)│
  │                                        │                   │
  │    When Cart Service restarts:         │                   │
  │                                        │  Consume event    │
  │                                        ├──────────────────►│
  │                                        │  Update prices    │
  │                                        │  in all carts     │
  │                                        │                   │
  │    CONSEQUENCE (during downtime):      │                   │
  │    - Carts show stale prices           │                   │
  │    - Eventually consistent after       │                   │
  │      Cart Service recovers            │                   │
```

---

### Scenario 3: Product Deletion Cascade

#### SUCCESS Flow

```
 Admin           Product Service       Kafka         Inventory Service    Cart Service
  │                    │                 │                  │                  │
  │ DELETE /products/  │                 │                  │                  │
  │         {id}       │                 │                  │                  │
  ├───────────────────►│                 │                  │                  │
  │                    │                 │                  │                  │
  │                    │ Soft-delete in  │                  │                  │
  │                    │ PostgreSQL      │                  │                  │
  │                    │────────┐        │                  │                  │
  │                    │◄───────┘        │                  │                  │
  │                    │                 │                  │                  │
  │                    │ ProductDeleted  │                  │                  │
  │                    │ Event           │                  │                  │
  │                    ├────────────────►│                  │                  │
  │                    │                 │                  │                  │
  │ 200 OK             │                 │  ┌──────────────►│                  │
  │◄───────────────────┤                 │  │               │                  │
  │                    │                 │  │  Delete        │                  │
  │                    │                 │  │  inventory     │                  │
  │                    │                 │  │  record        │                  │
  │                    │                 │  │               │                  │
  │                    │                 │  │               │                  │
  │                    │                 │  └───────────────►│                  │
  │                    │                 │                  │                  │
  │                    │                 │  ┌──────────────────────────────────►│
  │                    │                 │  │                                   │
  │                    │                 │  │  Remove product from all          │
  │                    │                 │  │  active carts                     │
  │                    │                 │  │  Evict cart cache                 │
  │                    │                 │  │                                   │
  │                    │                 │  └──────────────────────────────────►│
```

---

## Redis Caching Strategies

### Cache Architecture Per Service

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              REDIS (:6379)                                 │
│                                                                            │
│  ┌──────────────────────────┐     ┌──────────────────────────────────────┐ │
│  │    USER SERVICE KEYS     │     │      PRODUCT SERVICE KEYS           │ │
│  │                          │     │                                      │ │
│  │  denylist:session:{sid}  │     │  product:id::{productId}   TTL: 1h  │ │
│  │    TTL: matches token    │     │  product:slug::{slug}      TTL: 1h  │ │
│  │    expiration            │     │  category:id::{catId}      TTL: 6h  │ │
│  │                          │     │  category:slug::{slug}     TTL: 6h  │ │
│  │  Purpose: Immediate      │     │  category:tree::all        TTL: 6h  │ │
│  │  token revocation        │     │                                      │ │
│  └──────────────────────────┘     │  Purpose: Read-heavy catalog         │ │
│                                    │  caching with long TTLs             │ │
│  ┌──────────────────────────┐     └──────────────────────────────────────┘ │
│  │  INVENTORY SERVICE KEYS  │                                              │
│  │                          │     ┌──────────────────────────────────────┐ │
│  │  inventory::{productId}  │     │       CART SERVICE KEYS              │ │
│  │    TTL: 5 minutes        │     │                                      │ │
│  │                          │     │  cart::{userId}             TTL: 30m │ │
│  │  Purpose: Short TTL for  │     │                                      │ │
│  │  frequently changing     │     │  Purpose: Session-like cart          │ │
│  │  stock data              │     │  caching for active shoppers         │ │
│  └──────────────────────────┘     └──────────────────────────────────────┘ │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Scenario 4: Redis Cache Hit vs Miss (Product Service)

#### Cache HIT (fast path)

```
 Client              Product Service              Redis              PostgreSQL
  │                        │                        │                     │
  │ GET /products/{id}     │                        │                     │
  ├───────────────────────►│                        │                     │
  │                        │                        │                     │
  │                        │  GET product:id::{id}  │                     │
  │                        ├───────────────────────►│                     │
  │                        │                        │                     │
  │                        │  Cache HIT!            │                     │
  │                        │  Return cached product │                     │
  │                        │◄───────────────────────┤                     │
  │                        │                        │                     │
  │  200 OK (fast ~2ms)    │                        │                     │
  │◄───────────────────────┤                        │                     │
  │                        │                        │                     │
  │       No database query needed!                 │                     │
```

#### Cache MISS (slow path)

```
 Client              Product Service              Redis              PostgreSQL
  │                        │                        │                     │
  │ GET /products/{id}     │                        │                     │
  ├───────────────────────►│                        │                     │
  │                        │                        │                     │
  │                        │  GET product:id::{id}  │                     │
  │                        ├───────────────────────►│                     │
  │                        │                        │                     │
  │                        │  Cache MISS            │                     │
  │                        │◄───────────────────────┤                     │
  │                        │                        │                     │
  │                        │  SELECT * FROM product │                     │
  │                        ├────────────────────────┼────────────────────►│
  │                        │                        │                     │
  │                        │  Product data          │                     │
  │                        │◄───────────────────────┼─────────────────────┤
  │                        │                        │                     │
  │                        │  SET product:id::{id}  │                     │
  │                        │  TTL: 1 hour           │                     │
  │                        ├───────────────────────►│                     │
  │                        │                        │                     │
  │  200 OK (slower ~50ms) │                        │                     │
  │◄───────────────────────┤                        │                     │
```

#### FAILURE Flow - Redis Down

```
 Client              Product Service              Redis              PostgreSQL
  │                        │                        │                     │
  │ GET /products/{id}     │                        │                     │
  ├───────────────────────►│                        │                     │
  │                        │                        │                     │
  │                        │  GET product:id::{id}  │                     │
  │                        ├───────────────────────►│                     │
  │                        │                        │                     │
  │                        │  Connection refused!   │                     │
  │                        │◄──────────X            │                     │
  │                        │                        │                     │
  │                        │  Fallback: query DB    │                     │
  │                        ├────────────────────────┼────────────────────►│
  │                        │                        │                     │
  │                        │  Product data          │                     │
  │                        │◄───────────────────────┼─────────────────────┤
  │                        │                        │                     │
  │  200 OK (works but     │                        │                     │
  │  higher latency,       │                        │                     │
  │  more DB load)         │                        │                     │
  │◄───────────────────────┤                        │                     │
  │                        │                        │                     │
  │  CONSEQUENCE: Service still works, but:         │                     │
  │  - Every request hits PostgreSQL                │                     │
  │  - Higher latency, potential DB overload        │                     │
  │  - Token denylist in User Service FAILS         │                     │
  │    (revoked tokens may still be accepted!)      │                     │
```

---

### Scenario 5: Token Revocation (User Service + Redis)

#### SUCCESS - Logout with Denylist

```
 User               User Service                Redis              Other Services
  │                      │                        │                      │
  │ POST /auth/logout    │                        │                      │
  │ Cookie: JWT token    │                        │                      │
  ├─────────────────────►│                        │                      │
  │                      │                        │                      │
  │                      │ Extract sessionId      │                      │
  │                      │ from JWT               │                      │
  │                      │                        │                      │
  │                      │ SET denylist:session:   │                      │
  │                      │ {sessionId}            │                      │
  │                      │ TTL = token remaining  │                      │
  │                      │ expiry time            │                      │
  │                      ├───────────────────────►│                      │
  │                      │                        │                      │
  │                      │ Delete refresh token   │                      │
  │                      │ from PostgreSQL        │                      │
  │                      │                        │                      │
  │  200 OK              │                        │                      │
  │  (clear cookies)     │                        │                      │
  │◄─────────────────────┤                        │                      │
  │                      │                        │                      │
  │                      │                        │                      │
  │ Later: Try to use    │                        │                      │
  │ old token on any     │                        │                      │
  │ service              │                        │                      │
  ├──────────────────────┼────────────────────────┼─────────────────────►│
  │                      │                        │                      │
  │                      │                        │  Check denylist      │
  │                      │                        │◄─────────────────────┤
  │                      │                        │                      │
  │                      │                        │  FOUND! Token is     │
  │                      │                        │  revoked             │
  │                      │                        ├─────────────────────►│
  │                      │                        │                      │
  │  401 Unauthorized    │                        │                      │
  │◄─────────────────────┼────────────────────────┼──────────────────────┤
```

---

## MinIO Object Storage

### Scenario 6: Product Image Upload

#### SUCCESS Flow

```
 Admin           Product Service              MinIO                  PostgreSQL
  │                    │                        │                        │
  │ POST /products/    │                        │                        │
  │  {id}/images       │                        │                        │
  │ [multipart file]   │                        │                        │
  ├───────────────────►│                        │                        │
  │                    │                        │                        │
  │                    │ Validate:              │                        │
  │                    │ - Content type = image │                        │
  │                    │ - Size <= 5MB          │                        │
  │                    │                        │                        │
  │                    │ Generate object name:  │                        │
  │                    │ products/{UUID}.jpg    │                        │
  │                    │                        │                        │
  │                    │ PUT object to bucket   │                        │
  │                    │ "product-images"       │                        │
  │                    ├───────────────────────►│                        │
  │                    │                        │                        │
  │                    │ 200 OK (stored)        │                        │
  │                    │◄───────────────────────┤                        │
  │                    │                        │                        │
  │                    │ Save image metadata    │                        │
  │                    │ (url, isPrimary,       │                        │
  │                    │  displayOrder)         │                        │
  │                    ├────────────────────────┼───────────────────────►│
  │                    │                        │                        │
  │  201 Created       │                        │                        │
  │  {imageUrl:        │                        │                        │
  │   "products/       │                        │                        │
  │    abc-123.jpg"}   │                        │                        │
  │◄───────────────────┤                        │                        │
```

#### Serving Images - Presigned URL Flow

```
 Client           Product Service              MinIO
  │                    │                        │
  │ GET /products/{id} │                        │
  ├───────────────────►│                        │
  │                    │                        │
  │                    │ Get presigned URL      │
  │                    │ for each image         │
  │                    ├───────────────────────►│
  │                    │                        │
  │                    │ Signed URL with        │
  │                    │ expiry time            │
  │                    │◄───────────────────────┤
  │                    │                        │
  │  200 OK            │                        │
  │  {images: [        │                        │
  │    {url:           │                        │
  │     "http://minio  │                        │
  │     :9000/product  │                        │
  │     -images/...    │                        │
  │     ?X-Amz-        │                        │
  │     Signature=..." │                        │
  │    }               │                        │
  │  ]}                │                        │
  │◄───────────────────┤                        │
  │                    │                        │
  │ GET signed URL     │                        │
  │ (direct to MinIO)  │                        │
  ├────────────────────┼───────────────────────►│
  │                    │                        │
  │  Image binary data │                        │
  │◄───────────────────┼────────────────────────┤
```

#### FAILURE Flow - File Too Large

```
 Admin           Product Service
  │                    │
  │ POST /products/    │
  │  {id}/images       │
  │ [10MB file]        │
  ├───────────────────►│
  │                    │
  │                    │ Validate size:
  │                    │ 10MB > 5MB limit
  │                    │
  │  400 Bad Request   │
  │  "File size        │
  │   exceeds 5MB"     │
  │◄───────────────────┤
  │                    │
  │  No MinIO upload   │
  │  attempted         │
```

---

## gRPC Communication

### Scenario 7: Add Item to Cart (Full Flow)

#### SUCCESS Flow

```
 User              Cart Service         Product Service(gRPC)   Inventory Service(gRPC)
  │                     │                       │                        │
  │ POST /cart/items    │                       │                        │
  │ {productId,         │                       │                        │
  │  quantity: 2}       │                       │                        │
  ├────────────────────►│                       │                        │
  │                     │                       │                        │
  │                     │  GetProduct(id)       │                        │
  │                     │  [gRPC :9090]         │                        │
  │                     ├──────────────────────►│                        │
  │                     │                       │                        │
  │                     │  ProductResponse      │                        │
  │                     │  {name, price: 29.99, │                        │
  │                     │   images, active}     │                        │
  │                     │◄──────────────────────┤                        │
  │                     │                       │                        │
  │                     │  CheckStock(id, qty)  │                        │
  │                     │  [gRPC :9091]         │                        │
  │                     ├───────────────────────┼───────────────────────►│
  │                     │                       │                        │
  │                     │                       │  Check:                │
  │                     │                       │  available = quantity  │
  │                     │                       │   - reservedQuantity   │
  │                     │                       │  available(95) >= 2    │
  │                     │                       │                        │
  │                     │  CheckStockResponse   │                        │
  │                     │  {available: true,    │                        │
  │                     │   availableQty: 95}   │                        │
  │                     │◄──────────────────────┼────────────────────────┤
  │                     │                       │                        │
  │                     │  Add to cart:         │                        │
  │                     │  Save CartItem to DB  │                        │
  │                     │  ┌──────────┐         │                        │
  │                     │  │PostgreSQL│         │                        │
  │                     │  └──────────┘         │                        │
  │                     │                       │                        │
  │                     │  Update Redis cache   │                        │
  │                     │  ┌──────┐             │                        │
  │                     │  │Redis │             │                        │
  │                     │  └──────┘             │                        │
  │                     │                       │                        │
  │                     │  CartItemAddedEvent   │                        │
  │                     │  ──────► Kafka        │                        │
  │                     │                       │                        │
  │  200 OK             │                       │                        │
  │  {cart with items}  │                       │                        │
  │◄────────────────────┤                       │                        │
```

#### FAILURE Flow - Out of Stock

```
 User              Cart Service         Product Service(gRPC)   Inventory Service(gRPC)
  │                     │                       │                        │
  │ POST /cart/items    │                       │                        │
  │ {productId,         │                       │                        │
  │  quantity: 200}     │                       │                        │
  ├────────────────────►│                       │                        │
  │                     │                       │                        │
  │                     │  GetProduct(id)       │                        │
  │                     ├──────────────────────►│                        │
  │                     │  ProductResponse OK   │                        │
  │                     │◄──────────────────────┤                        │
  │                     │                       │                        │
  │                     │  CheckStock(id, 200)  │                        │
  │                     ├───────────────────────┼───────────────────────►│
  │                     │                       │                        │
  │                     │                       │  available(95) < 200   │
  │                     │                       │                        │
  │                     │  {available: false,   │                        │
  │                     │   availableQty: 95}   │                        │
  │                     │◄──────────────────────┼────────────────────────┤
  │                     │                       │                        │
  │  400 Bad Request    │                       │                        │
  │  "Insufficient      │                       │                        │
  │   stock. Only 95    │                       │                        │
  │   available"        │                       │                        │
  │◄────────────────────┤                       │                        │
  │                     │                       │                        │
  │  No cart changes.   │                       │                        │
  │  No Kafka event.    │                       │                        │
```

#### FAILURE Flow - Product Service Unreachable (gRPC)

```
 User              Cart Service         Product Service(gRPC)
  │                     │                       │
  │ POST /cart/items    │                       │
  │ {productId,         │                       │
  │  quantity: 2}       │                       │
  ├────────────────────►│                       │
  │                     │                       │
  │                     │  GetProduct(id)       │
  │                     │  [gRPC :9090]         │
  │                     ├──────────────────────►│
  │                     │                       │
  │                     │  UNAVAILABLE          │
  │                     │  Connection refused   │
  │                     │◄─────────X            │
  │                     │                       │
  │  503 Service        │                       │
  │  Unavailable        │                       │
  │  "Product service   │                       │
  │   is unavailable"   │                       │
  │◄────────────────────┤                       │
  │                     │                       │
  │  Cart is unchanged. │                       │
  │  gRPC failure       │                       │
  │  prevents stale     │                       │
  │  data in cart.      │                       │
```

---

## End-to-End Scenarios

### Scenario 8: Stock Reservation (Order Checkout Flow)

#### SUCCESS Flow

```
 User           Cart Service      Inventory Service(gRPC)    Kafka         Product Service
  │                  │                    │                     │                 │
  │ POST /cart/      │                    │                     │                 │
  │  checkout        │                    │                     │                 │
  ├─────────────────►│                    │                     │                 │
  │                  │                    │                     │                 │
  │                  │ For each cart item:│                     │                 │
  │                  │                    │                     │                 │
  │                  │ ReserveStock       │                     │                 │
  │                  │ (orderId,          │                     │                 │
  │                  │  productId,        │                     │                 │
  │                  │  quantity)         │                     │                 │
  │                  ├───────────────────►│                     │                 │
  │                  │                    │                     │                 │
  │                  │                    │ Create reservation: │                 │
  │                  │                    │ status = PENDING    │                 │
  │                  │                    │ expiresAt = now()   │                 │
  │                  │                    │          + 15min    │                 │
  │                  │                    │ ┌──────────┐        │                 │
  │                  │                    │ │PostgreSQL│        │                 │
  │                  │                    │ └──────────┘        │                 │
  │                  │                    │                     │                 │
  │                  │                    │ Update inventory:   │                 │
  │                  │                    │ reservedQty += qty  │                 │
  │                  │                    │                     │                 │
  │                  │                    │ Evict Redis cache   │                 │
  │                  │                    │ ┌──────┐            │                 │
  │                  │                    │ │Redis │            │                 │
  │                  │                    │ └──────┘            │                 │
  │                  │                    │                     │                 │
  │                  │ Reserve OK         │                     │                 │
  │                  │◄──────────────────┤                     │                 │
  │                  │                    │                     │                 │
  │                  │                    │ StockReservedEvent  │                 │
  │                  │                    ├────────────────────►│                 │
  │                  │                    │                     │                 │
  │                  │ CartConvertedEvent │                     │                 │
  │                  ├────────────────────┼────────────────────►│                 │
  │                  │                    │                     │                 │
  │  200 OK          │                    │                     │                 │
  │  {orderId}       │                    │                     │                 │
  │◄─────────────────┤                    │                     │                 │
  │                  │                    │                     │                 │
  │                  │                    │                     │                 │
  │   ... Payment succeeds ...           │                     │                 │
  │                  │                    │                     │                 │
  │                  │ ConfirmStock       │                     │                 │
  │                  │ (orderId,          │                     │                 │
  │                  │  productId)        │                     │                 │
  │                  ├───────────────────►│                     │                 │
  │                  │                    │                     │                 │
  │                  │                    │ Reservation →       │                 │
  │                  │                    │   CONFIRMED         │                 │
  │                  │                    │ quantity -= qty     │                 │
  │                  │                    │ reservedQty -= qty  │                 │
  │                  │                    │                     │                 │
  │                  │                    │ StockConfirmedEvent │                 │
  │                  │                    ├────────────────────►│                 │
  │                  │                    │                     │                 │
  │                  │                    │ StockUpdatedEvent   │                 │
  │                  │                    ├────────────────────►│                 │
  │                  │                    │                     │  Consume        │
  │                  │                    │                     ├────────────────►│
  │                  │                    │                     │                 │
  │                  │                    │                     │  Update product │
  │                  │                    │                     │  stockStatus    │
  │                  │                    │                     │  (may become    │
  │                  │                    │                     │   LOW_STOCK or  │
  │                  │                    │                     │   OUT_OF_STOCK) │
```

#### FAILURE Flow - Payment Fails (Stock Release)

```
 User           Cart Service      Inventory Service(gRPC)    Kafka
  │                  │                    │                     │
  │ POST /cart/      │                    │                     │
  │  checkout        │                    │                     │
  ├─────────────────►│                    │                     │
  │                  │                    │                     │
  │                  │ ReserveStock OK    │                     │
  │                  │◄──────────────────►│                     │
  │                  │                    │                     │
  │  200 OK          │                    │                     │
  │◄─────────────────┤                    │                     │
  │                  │                    │                     │
  │                  │                    │                     │
  │   ... Payment FAILS ...              │                     │
  │                  │                    │                     │
  │                  │ ReleaseStock       │                     │
  │                  │ (orderId,          │                     │
  │                  │  productId)        │                     │
  │                  ├───────────────────►│                     │
  │                  │                    │                     │
  │                  │                    │ Reservation →       │
  │                  │                    │   RELEASED          │
  │                  │                    │ reservedQty -= qty  │
  │                  │                    │                     │
  │                  │                    │ StockReleasedEvent  │
  │                  │                    ├────────────────────►│
  │                  │                    │                     │
  │                  │ Released OK        │                     │
  │                  │◄──────────────────┤                     │
  │                  │                    │                     │
  │  Stock is now available for other    │                     │
  │  customers again.                    │                     │
  │                  │                    │                     │
  │                  │                    │                     │
  │  EDGE CASE: Reservation Expiry (15 min timeout)           │
  │  If neither confirm nor release happens within 15 min:    │
  │  - Scheduled job releases expired reservations            │
  │  - StockReleasedEvent is published                        │
  │  - Stock becomes available again automatically            │
```

---

### Scenario 9: Full User Journey (All Services)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        COMPLETE USER JOURNEY                               │
└─────────────────────────────────────────────────────────────────────────────┘

Step 1: REGISTER & LOGIN (User Service)
═══════════════════════════════════════════════════
  User ──POST /auth/register──► User Service ──► PostgreSQL (save user)
  User ──POST /auth/login──────► User Service ──► PostgreSQL (create session)
                                      │
                                      ├──► Redis (cache session)
                                      └──► Return JWT (HttpOnly cookie)

Step 2: BROWSE PRODUCTS (Product Service)
═══════════════════════════════════════════════════
  User ──GET /products──────────► Product Service
                                      │
                                      ├──► Redis CACHE HIT?
                                      │      YES → return cached data
                                      │      NO  → PostgreSQL → cache in Redis
                                      │
                                      └──► MinIO (presigned URLs for images)

Step 3: ADD TO CART (Cart + Product + Inventory)
═══════════════════════════════════════════════════
  User ──POST /cart/items──────► Cart Service
                                      │
                                      ├──gRPC──► Product Service (get details)
                                      ├──gRPC──► Inventory Service (check stock)
                                      │
                                      ├──► PostgreSQL (save cart item)
                                      ├──► Redis (cache cart)
                                      └──► Kafka (CartItemAddedEvent)

Step 4: CHECKOUT (Cart + Inventory)
═══════════════════════════════════════════════════
  User ──POST /cart/checkout───► Cart Service
                                      │
                                      ├──gRPC──► Inventory Service
                                      │            (ReserveStock - 15min lock)
                                      │
                                      ├──► Kafka (CartConvertedEvent)
                                      └──► Cart status → CONVERTED

Step 5: PAYMENT SUCCESS (Inventory + Product)
═══════════════════════════════════════════════════
  Payment ──ConfirmStock───────► Inventory Service
                                      │
                                      ├──► PostgreSQL (deduct stock)
                                      ├──► Redis (evict cache)
                                      └──► Kafka (StockConfirmedEvent,
                                                   StockUpdatedEvent)
                                                        │
                                              Product Service (update
                                              stockStatus in catalog)

  OR

Step 5b: PAYMENT FAILURE
═══════════════════════════════════════════════════
  Payment ──ReleaseStock───────► Inventory Service
                                      │
                                      ├──► PostgreSQL (release reservation)
                                      ├──► Redis (evict cache)
                                      └──► Kafka (StockReleasedEvent)

Step 6: LOGOUT (User Service)
═══════════════════════════════════════════════════
  User ──POST /auth/logout─────► User Service
                                      │
                                      ├──► Redis (SET denylist:session:{id})
                                      └──► PostgreSQL (delete refresh token)
```

---

## Failure Recovery Summary

| Failure Scenario | Impact | Recovery Mechanism |
| --- | --- | --- |
| **Kafka broker down** | Events not delivered; inventory not created for new products | Kafka retries + manual admin intervention |
| **Redis down** | Higher latency; all requests hit DB; token denylist fails (security risk) | Graceful degradation to DB; restart Redis |
| **MinIO down** | Image uploads fail; existing presigned URLs expire | Retry uploads; MinIO data persists on disk |
| **Product Service down** | Cart can't validate products (gRPC fails); no new products | Cart returns 503; cached data still served |
| **Inventory Service down** | Cart can't check stock (gRPC fails); no reservations | Cart returns 503; existing reservations expire in 15min |
| **Cart Service down** | Users can't manage carts; Kafka events queue up | Kafka retains events; processed on recovery |
| **User Service down** | No login/register; existing JWTs still valid until expiry | Stateless JWT allows other services to function |
| **PostgreSQL down** | Complete data loss scenario; all services fail | All services return 500; requires DB recovery |
| **Reservation expires (15min)** | Held stock released back to available | Scheduled job auto-releases; StockReleasedEvent published |
| **Duplicate Kafka events** | Potential double-processing | Idempotent consumers (check if already processed) |
