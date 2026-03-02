# Prompt: Create Payment Service for E-Commerce Microservices App

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
- Kafka PRODUCER: publishes to `product-events` topic
- gRPC SERVER: exposes product data to other services (port 9090)
- MinIO: stores product images

**3. Inventory Service** (port 8083, gRPC port 9091, context-path: /api/v1)
- Manages product stock levels and reservations
- PostgreSQL DB: `inventory_service_db`
- Redis: caches stock info per product
- Kafka PRODUCER: publishes to `inventory-events` topic
- gRPC SERVER: exposes stock operations

**4. Cart Service** (port 8084, context-path: /api/v1)
- Manages shopping carts per user
- PostgreSQL DB: `cart_service_db`
- Redis: caches cart data
- Kafka PRODUCER: publishes to `cart-events` topic
- gRPC CLIENT: calls Product Service, Inventory Service

**5. Order Service** (port 8085, context-path: /api/v1)
- Manages orders and order lifecycle
- PostgreSQL DB: `order_service_db`
- Redis: caches order data
- Kafka PRODUCER: publishes to `order-events` topic (ORDER_CREATED, ORDER_CONFIRMED, ORDER_CANCELLED, ORDER_SHIPPED, ORDER_DELIVERED)
- Kafka CONSUMER: listens to `inventory-events` and `product-events`
- gRPC CLIENT: calls Product Service, Inventory Service
- REST CLIENT: calls Cart Service

**Order Status Flow:**
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
PENDING → CANCELLED
CONFIRMED → CANCELLED
PROCESSING → CANCELLED
CANCELLED → REFUNDED
```

**Order Events Published (topic: `order-events`):**
```json
ORDER_CREATED: {eventId, eventType, orderId, orderNumber, userId, items[], totalAmount, finalAmount, status, timestamp}
ORDER_CONFIRMED: {eventId, eventType, orderId, orderNumber, userId, confirmedAt, timestamp}
ORDER_CANCELLED: {eventId, eventType, orderId, orderNumber, userId, reason, cancelledAt, timestamp}
ORDER_SHIPPED: {eventId, eventType, orderId, orderNumber, userId, shippedAt, timestamp}
ORDER_DELIVERED: {eventId, eventType, orderId, orderNumber, userId, deliveredAt, timestamp}
```

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

## PAYMENT GATEWAY: KAPITAL BANK (Azerbaijan)

### Overview

Kapital Bank provides an e-commerce payment gateway with an XML/SOAP-based API. The payment flow is redirect-based: the merchant creates an order via API, redirects the customer to the bank's payment page, and receives the result via callback URLs.

### API Details

**Test Environment:**
- API Endpoint: `https://tstpg.kapitalbank.az:5443/Exec`
- Payment Page: `https://tstpg.kapitalbank.az/index.jsp?ORDERID={orderId}&SESSIONID={sessionId}`

**Production Environment:**
- API Endpoint: `https://e-commerce.kapitalbank.az:5443/Exec`
- Payment Page: `https://e-commerce.kapitalbank.az/index.jsp?ORDERID={orderId}&SESSIONID={sessionId}`

**Authentication:** SSL certificate-based (merchant `.crt` and `.key` files)

**Protocol:** XML over HTTPS (port 5443)

**Test Merchant ID:** `E1000010`

**Currency Code:** `944` (AZN — Azerbaijani Manat)

### API Operations

#### 1. CreateOrder — Initiate a payment

**Request:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<TKKPG>
  <Request>
    <Operation>CreateOrder</Operation>
    <Language>EN</Language>
    <Order>
      <OrderType>Purchase</OrderType>
      <Merchant>E1000010</Merchant>
      <Amount>10000</Amount>
      <Currency>944</Currency>
      <Description>Order ORD-20260301-001</Description>
      <ApproveURL>https://yoursite.com/api/v1/payments/callback/approve</ApproveURL>
      <CancelURL>https://yoursite.com/api/v1/payments/callback/cancel</CancelURL>
      <DeclineURL>https://yoursite.com/api/v1/payments/callback/decline</DeclineURL>
    </Order>
  </Request>
</TKKPG>
```

**IMPORTANT:** Amount is in minor units (tiyin). 100.00 AZN = `10000`.

**Response:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<TKKPG>
  <Response>
    <Operation>CreateOrder</Operation>
    <Status>00</Status>
    <Order>
      <OrderID>12345678</OrderID>
      <SessionID>ABCDEF123456</SessionID>
      <URL>https://tstpg.kapitalbank.az/index.jsp</URL>
    </Order>
  </Response>
</TKKPG>
```

After receiving the response, redirect the user to:
`{URL}?ORDERID={OrderID}&SESSIONID={SessionID}`

#### 2. GetOrderStatus — Check payment result

**Request:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<TKKPG>
  <Request>
    <Operation>GetOrderStatus</Operation>
    <Language>EN</Language>
    <Order>
      <Merchant>E1000010</Merchant>
      <OrderID>12345678</OrderID>
    </Order>
    <SessionID>ABCDEF123456</SessionID>
  </Request>
</TKKPG>
```

**Response contains:** OrderStatus (APPROVED, DECLINED, CANCELED, etc.), Amount, Currency, transaction details.

#### 3. GetOrderInformation — Get detailed transaction info

Same structure as GetOrderStatus but returns additional details: fees, dates, card info (masked), refund info.

#### 4. ReverseOrder — Refund a payment

**Request:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<TKKPG>
  <Request>
    <Operation>Reverse</Operation>
    <Language>EN</Language>
    <Order>
      <Merchant>E1000010</Merchant>
      <OrderID>12345678</OrderID>
    </Order>
    <SessionID>ABCDEF123456</SessionID>
    <Description>Refund for cancelled order</Description>
  </Request>
</TKKPG>
```

### Payment Flow

```
1. Customer creates order in Order Service → status: PENDING
2. Customer calls POST /payments/initiate with orderNumber
3. Payment Service calls Kapital Bank CreateOrder API (XML over HTTPS with SSL cert)
4. Kapital Bank returns OrderID + SessionID + payment page URL
5. Payment Service saves payment record (INITIATED) and returns payment URL to customer
6. Customer is redirected to Kapital Bank payment page
7. Customer enters card details on bank's page
8. Bank processes payment and redirects customer to one of:
   - ApproveURL → payment successful
   - CancelURL → customer cancelled
   - DeclineURL → payment declined
9. Payment Service receives the callback with ORDERID and SESSIONID
10. Payment Service calls GetOrderStatus to verify the payment
11. Payment Service updates payment record status
12. Payment Service publishes PAYMENT_SUCCESS or PAYMENT_FAILED event to Kafka
13. Order Service consumes the event and auto-confirms or auto-cancels the order
```

### SSL Certificate Configuration

The merchant SSL certificate files (`.crt` and `.key`) are required for API communication. Store them in:
- **Local development:** `src/main/resources/certificates/` directory
- **Docker:** Mount as volume or store paths in Vault
- **Vault secrets:** Store certificate file paths at `secret/payment-service`

Use Java's `SSLContext` with a custom `KeyManager` to load the certificate and key for HTTPS connections.

---

## SECURITY PATTERN (Shared JWT Secret via Vault)

All services share the same JWT access secret. Each service has its own copy stored in Vault at `secret/<service-name> → jwt.access.secret` (same value across all). Each service validates tokens independently.

Pattern from existing services:
- `JwtUtil.java` — validates access tokens only (no token generation)
- `JwtAuthenticationFilter.java` — extracts token from `access_token` cookie OR `Authorization: Bearer` header, validates, sets SecurityContext with userId (UUID) and `ROLE_<role>` authority
- `SecurityConfig.java` — STATELESS sessions, permits public endpoints, role-based method security with `@PreAuthorize`

**IMPORTANT:** The callback URLs (approve/cancel/decline) must be publicly accessible — do NOT require JWT authentication for these endpoints. Use a shared secret or HMAC signature to verify callback authenticity instead.

---

## VAULT SECRET STRUCTURE

```bash
vault kv put secret/payment-service \
  spring.datasource.url="jdbc:postgresql://postgres:5432/payment_service_db" \
  spring.datasource.username="payment_service_db" \
  spring.datasource.password="payment_service_db_password_12345" \
  spring.data.redis.host="redis" \
  spring.data.redis.port="6379" \
  spring.data.redis.password="" \
  jwt.access.secret="at-least-64-character-very-long-and-secure-secret-key-for-hs512-logic-2026" \
  kapitalbank.merchant-id="E1000010" \
  kapitalbank.cert-path="/app/certificates/merchant.crt" \
  kapitalbank.key-path="/app/certificates/merchant.key"
```

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

## REDIS CONFIGURATION PATTERN (from existing services)

Use `GenericJackson2JsonRedisSerializer` WITHOUT `activateDefaultTyping` (to avoid issues with Java records):

```java
@Bean
public RedisSerializer<Object> jsonRedisSerializer() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return new GenericJackson2JsonRedisSerializer(objectMapper);
}
```

**NOTE:** Do NOT use `@Cacheable` on methods that return Java records or generic types like `PagedResponse<T>` — it causes `ClassCastException` (LinkedHashMap cannot be cast). Use caching only for simple lookups or handle serialization manually.

---

## WHAT I NEED YOU TO CREATE: PAYMENT SERVICE

### Service Details
- **Name:** payment-service
- **Port:** 8086 (REST)
- **Context path:** /api/v1
- **Base package:** `com.ecommerce.paymentservice`
- **Database:** `payment_service_db` (PostgreSQL, same instance)
- **Vault path:** `secret/payment-service`

### Technology Stack (must match existing services exactly)
- Java 21
- Spring Boot 4.0.2
- Gradle 9.3
- Spring Cloud 2025.1.0
- Kafka: spring-boot-starter-kafka (with custom non-deprecated serializer/deserializer)
- Redis: spring-boot-starter-data-redis (for caching payment data)
- PostgreSQL: via spring-boot-starter-data-jpa
- Vault: spring-cloud-starter-vault-config (KV v2, `backend-version: 2`)
- JWT: jjwt 0.13.0
- MapStruct: 1.6.3
- Lombok
- Swagger: springdoc-openapi-starter-webmvc-ui 2.8.14

### Payment Service does NOT need gRPC
The payment service is REST-only. It communicates with other services via Kafka events.

---

### Database Schema

**payments table:**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Unique payment ID |
| order_number | VARCHAR(50) | NOT NULL, INDEXED | Reference to order in Order Service |
| user_id | UUID | NOT NULL, INDEXED | Reference to user |
| amount | DECIMAL(12,2) | NOT NULL | Payment amount |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'AZN' | ISO 4217 currency code |
| status | VARCHAR(30) | NOT NULL, DEFAULT 'INITIATED' | Payment status |
| kapital_order_id | VARCHAR(50) | UNIQUE | Kapital Bank's order ID |
| kapital_session_id | VARCHAR(100) | | Kapital Bank's session ID |
| payment_url | VARCHAR(500) | | Redirect URL for customer |
| description | VARCHAR(500) | | Payment description |
| error_message | VARCHAR(500) | | Error details if payment failed |
| created_at | TIMESTAMP | @CreationTimestamp | |
| updated_at | TIMESTAMP | @UpdateTimestamp | |

**payment_status_history table:**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | |
| payment_id | BIGINT | NOT NULL, FK to payments, INDEXED | |
| previous_status | VARCHAR(30) | | Previous status |
| new_status | VARCHAR(30) | NOT NULL | New status |
| reason | VARCHAR(500) | | Reason for status change |
| created_at | TIMESTAMP | @CreationTimestamp | |

**PaymentStatus enum:**
```java
public enum PaymentStatus {
    INITIATED,      // Payment record created, waiting for customer to pay
    PROCESSING,     // Customer redirected to bank, payment in progress
    APPROVED,       // Payment approved by bank
    DECLINED,       // Payment declined by bank
    CANCELLED,      // Customer cancelled on bank page
    REFUNDED,       // Payment was refunded
    ERROR           // System error during payment
}
```

---

### REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/payments/initiate` | CUSTOMER, STORE, ADMIN | Initiate payment for an order |
| GET | `/payments/{paymentId}` | CUSTOMER, STORE, ADMIN | Get payment details |
| GET | `/payments/order/{orderNumber}` | CUSTOMER, STORE, ADMIN | Get payment by order number |
| GET | `/payments/callback/approve` | PUBLIC (no JWT) | Kapital Bank approve callback |
| GET | `/payments/callback/cancel` | PUBLIC (no JWT) | Kapital Bank cancel callback |
| GET | `/payments/callback/decline` | PUBLIC (no JWT) | Kapital Bank decline callback |
| POST | `/payments/{paymentId}/refund` | ADMIN | Initiate refund |
| GET | `/payments/my` | CUSTOMER, STORE, ADMIN | Get current user's payments (paginated) |

**IMPORTANT:** The callback endpoints (`/payments/callback/*`) are called by Kapital Bank via browser redirect. They receive `ORDERID` and `SESSIONID` as query parameters. These must be publicly accessible (no JWT required). After processing, redirect the user to a frontend success/failure page.

---

### Request/Response DTOs

```java
// Request: Initiate payment
public record InitiatePaymentRequest(
    @NotBlank String orderNumber
) {}

// Response: Payment initiated
public record PaymentInitiatedResponse(
    Long paymentId,
    String orderNumber,
    PaymentStatus status,
    String paymentUrl,    // URL to redirect customer to bank payment page
    Instant createdAt
) {}

// Response: Payment details
public record PaymentResponse(
    Long id,
    String orderNumber,
    UUID userId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String kapitalOrderId,
    String description,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt
) {}

// Response: Payment status history
public record PaymentStatusHistoryResponse(
    PaymentStatus previousStatus,
    PaymentStatus newStatus,
    String reason,
    Instant changedAt
) {}

// Paginated response (same pattern as other services)
public record PagedResponse<T>(...) {}
```

---

### Kafka Events

**Payment Service PRODUCES to `payment-events` topic:**

| Event | When | Payload |
|-------|------|---------|
| PAYMENT_INITIATED | When customer starts payment | `{eventId, eventType, paymentId, orderNumber, userId, amount, currency, timestamp}` |
| PAYMENT_SUCCESS | When bank approves payment | `{eventId, eventType, paymentId, orderNumber, userId, amount, kapitalOrderId, approvedAt, timestamp}` |
| PAYMENT_FAILED | When bank declines/cancels payment | `{eventId, eventType, paymentId, orderNumber, userId, reason, failedAt, timestamp}` |
| PAYMENT_REFUNDED | When refund is processed | `{eventId, eventType, paymentId, orderNumber, userId, amount, refundedAt, timestamp}` |

**Payment Service CONSUMES from `order-events` topic (group: `payment-service-group`):**

| Event | Action |
|-------|--------|
| ORDER_CANCELLED | If payment exists and is APPROVED, auto-initiate refund via Kapital Bank ReverseOrder API |

---

### Core Business Logic

**Initiate Payment Flow (POST /payments/initiate):**
1. Extract userId from JWT SecurityContext
2. Validate orderNumber exists (could call Order Service REST or just trust the order number)
3. Check if a payment already exists for this order (prevent duplicates)
4. Convert amount to minor units (multiply by 100 for AZN → tiyin)
5. Build XML request for Kapital Bank CreateOrder
6. Send XML via HTTPS with SSL certificate to `https://tstpg.kapitalbank.az:5443/Exec`
7. Parse XML response to extract OrderID, SessionID, URL
8. Save payment record with status INITIATED
9. Record status history: null → INITIATED
10. Publish `PAYMENT_INITIATED` event to Kafka
11. Return PaymentInitiatedResponse with the payment URL
12. Customer is redirected to payment URL on frontend

**Approve Callback Flow (GET /payments/callback/approve?ORDERID=xxx&SESSIONID=xxx):**
1. Find payment by kapitalOrderId
2. Call Kapital Bank GetOrderStatus to verify the payment
3. If verified as APPROVED:
   a. Update payment status to APPROVED
   b. Record status history
   c. Publish `PAYMENT_SUCCESS` event to Kafka
4. Redirect user to frontend success page (e.g., `https://yoursite.com/payment/success?order={orderNumber}`)

**Cancel Callback Flow (GET /payments/callback/cancel?ORDERID=xxx&SESSIONID=xxx):**
1. Find payment by kapitalOrderId
2. Update payment status to CANCELLED
3. Record status history
4. Publish `PAYMENT_FAILED` event with reason "Customer cancelled"
5. Redirect user to frontend cancel page

**Decline Callback Flow (GET /payments/callback/decline?ORDERID=xxx&SESSIONID=xxx):**
1. Find payment by kapitalOrderId
2. Call Kapital Bank GetOrderStatus for details
3. Update payment status to DECLINED
4. Record status history with decline reason
5. Publish `PAYMENT_FAILED` event with reason "Payment declined"
6. Redirect user to frontend decline page

**Refund Flow (POST /payments/{paymentId}/refund):**
1. Find payment by ID
2. Validate status is APPROVED
3. Build XML request for Kapital Bank ReverseOrder
4. Send to Kapital Bank API
5. If successful, update status to REFUNDED
6. Record status history
7. Publish `PAYMENT_REFUNDED` event

---

### Kapital Bank Client Service

Create a dedicated client service for communicating with Kapital Bank's XML API:

```java
@Service
@Slf4j
public class KapitalBankClient {

    private final String apiUrl;        // from application.yml
    private final String merchantId;    // from Vault
    private final SSLContext sslContext; // built from cert + key files

    // Build SSLContext from certificate files in @PostConstruct

    public KapitalBankOrderResponse createOrder(BigDecimal amount, String description,
                                                 String approveUrl, String cancelUrl, String declineUrl) {
        // Build XML request
        // Send via HttpClient with SSL
        // Parse XML response
        // Return OrderID, SessionID, URL
    }

    public KapitalBankStatusResponse getOrderStatus(String orderId, String sessionId) {
        // Build XML request
        // Send via HttpClient with SSL
        // Parse XML response
        // Return status details
    }

    public KapitalBankRefundResponse reverseOrder(String orderId, String sessionId, String description) {
        // Build XML request
        // Send via HttpClient with SSL
        // Parse XML response
        // Return result
    }
}
```

**XML Building:** Use Java's built-in `javax.xml.parsers.DocumentBuilder` or simple string templates for XML construction. Use `javax.xml.parsers.DocumentBuilder` for XML parsing.

**HTTP Client:** Use Java 21's `java.net.http.HttpClient` with custom `SSLContext` for certificate-based authentication.

---

### Order Service Changes (when Payment Service is implemented)

The Order Service needs these modifications:

1. **Add Kafka consumer for `payment-events` topic:**
   - On `PAYMENT_SUCCESS` → call existing `confirmOrder()` logic internally (not via REST)
   - On `PAYMENT_FAILED` → call existing `cancelOrder()` logic with reason from event

2. **Add `payment-events` topic constant to `KafkaTopicConfig.java`:**
   ```java
   public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
   ```

3. **Create `PaymentEventConsumer.java` in kafka package:**
   ```java
   @KafkaListener(topics = KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, groupId = KafkaTopicConfig.ORDER_SERVICE_GROUP)
   public void consumePaymentEvents(JsonNode node) {
       String eventType = node.get("eventType").asText();
       if ("PAYMENT_SUCCESS".equals(eventType)) {
           // Extract orderNumber, call orderService.confirmOrder(...)
       } else if ("PAYMENT_FAILED".equals(eventType)) {
           // Extract orderNumber + reason, call orderService.cancelOrder(...)
       }
   }
   ```

4. **The manual `POST /orders/{orderNumber}/confirm` endpoint can remain** for admin use / testing, but the primary flow will be automated via Kafka.

---

### application.yml Configuration

```yaml
spring:
  application:
    name: payment-service

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
      host: ${SPRING_DATA_REDIS_HOST:localhost}
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
      uri: ${SPRING_CLOUD_VAULT_URI:http://localhost:8200}
      token: ${SPRING_CLOUD_VAULT_TOKEN:root-token}
      enabled: true
      fail-fast: true
      kv:
        enabled: true
        backend: secret
        default-context: payment-service
        backend-version: 2

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}

  web:
    error:
      include-message: always
      include-binding-errors: always

server:
  port: 8086
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

kapitalbank:
  api-url: ${KAPITALBANK_API_URL:https://tstpg.kapitalbank.az:5443/Exec}
  payment-page-url: ${KAPITALBANK_PAYMENT_PAGE_URL:https://tstpg.kapitalbank.az/index.jsp}
  merchant-id: ${kapitalbank.merchant-id:E1000010}
  currency: "944"
  language: "EN"
  cert-path: ${kapitalbank.cert-path:src/main/resources/certificates/merchant.crt}
  key-path: ${kapitalbank.key-path:src/main/resources/certificates/merchant.key}
  callback-base-url: ${KAPITALBANK_CALLBACK_BASE_URL:http://localhost:8086/api/v1}

frontend:
  success-url: ${FRONTEND_SUCCESS_URL:http://localhost:3000/payment/success}
  cancel-url: ${FRONTEND_CANCEL_URL:http://localhost:3000/payment/cancel}
  decline-url: ${FRONTEND_DECLINE_URL:http://localhost:3000/payment/decline}

logging:
  level:
    root: INFO
    com.ecommerce.paymentservice: DEBUG
    org.springframework.security: INFO
```

---

### Project Structure

```
payment-service/
├── build.gradle
├── settings.gradle
├── gradlew, gradlew.bat
├── gradle/wrapper/
├── Dockerfile
├── src/main/
│   ├── java/com/ecommerce/paymentservice/
│   │   ├── PaymentServiceApplication.java
│   │   ├── client/
│   │   │   └── KapitalBankClient.java          (XML/HTTPS client for Kapital Bank API)
│   │   ├── config/
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   ├── KafkaProducerConfig.java
│   │   │   ├── KafkaTopicConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── controller/
│   │   │   ├── PaymentController.java           (authenticated endpoints)
│   │   │   └── PaymentCallbackController.java   (public callback endpoints)
│   │   ├── dto/
│   │   │   ├── event/
│   │   │   │   ├── PaymentInitiatedEvent.java     (PRODUCED)
│   │   │   │   ├── PaymentSuccessEvent.java        (PRODUCED)
│   │   │   │   ├── PaymentFailedEvent.java         (PRODUCED)
│   │   │   │   ├── PaymentRefundedEvent.java       (PRODUCED)
│   │   │   │   ├── OrderCreatedEvent.java           (CONSUMED from Order Service)
│   │   │   │   └── OrderCancelledEvent.java         (CONSUMED from Order Service)
│   │   │   ├── request/
│   │   │   │   └── InitiatePaymentRequest.java
│   │   │   ├── response/
│   │   │   │   ├── PaymentResponse.java
│   │   │   │   ├── PaymentInitiatedResponse.java
│   │   │   │   ├── PaymentStatusHistoryResponse.java
│   │   │   │   └── PagedResponse.java
│   │   │   ├── kapitalbank/
│   │   │   │   ├── KapitalBankOrderResponse.java   (parsed from XML)
│   │   │   │   ├── KapitalBankStatusResponse.java  (parsed from XML)
│   │   │   │   └── KapitalBankRefundResponse.java  (parsed from XML)
│   │   │   └── common/
│   │   │       ├── ErrorResponse.java
│   │   │       └── ValidationErrorResponse.java
│   │   ├── entity/
│   │   │   ├── Payment.java
│   │   │   ├── PaymentStatus.java
│   │   │   └── PaymentStatusHistory.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── PaymentNotFoundException.java
│   │   │   ├── PaymentAlreadyExistsException.java
│   │   │   ├── InvalidPaymentStatusException.java
│   │   │   └── KapitalBankException.java
│   │   ├── kafka/
│   │   │   ├── PaymentEventProducer.java
│   │   │   └── OrderEventConsumer.java
│   │   ├── mapper/
│   │   │   └── PaymentMapper.java
│   │   ├── repository/
│   │   │   ├── PaymentRepository.java
│   │   │   └── PaymentStatusHistoryRepository.java
│   │   ├── security/
│   │   │   ├── JwtUtil.java
│   │   │   └── JwtAuthenticationFilter.java
│   │   └── service/
│   │       └── PaymentService.java
│   └── resources/
│       ├── application.yml
│       └── certificates/          (merchant SSL cert + key files)
│           ├── merchant.crt
│           └── merchant.key
└── src/test/
```

---

### Docker Compose Addition

```yaml
payment-service:
  build:
    context: ./payment-service
    dockerfile: Dockerfile
  container_name: payment-service
  restart: unless-stopped
  ports:
    - "8086:8086"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SPRING_CLOUD_VAULT_URI: http://vault:8200
    SPRING_CLOUD_VAULT_TOKEN: root-token
    SPRING_DATA_REDIS_HOST: redis
    KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    KAPITALBANK_API_URL: https://tstpg.kapitalbank.az:5443/Exec
    KAPITALBANK_CALLBACK_BASE_URL: https://yoursite.com/api/v1
  volumes:
    - ./payment-service/src/main/resources/certificates:/app/certificates:ro
  networks:
    - ecommerce-network
  depends_on:
    vault-init:
      condition: service_completed_successfully
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy
    kafka:
      condition: service_healthy
```

---

### Vault Init Addition

```bash
vault kv put secret/payment-service \
  spring.datasource.url="jdbc:postgresql://postgres:5432/payment_service_db" \
  spring.datasource.username="payment_service_db" \
  spring.datasource.password="payment_service_db_password_12345" \
  spring.data.redis.host="redis" \
  spring.data.redis.port="6379" \
  spring.data.redis.password="" \
  jwt.access.secret="at-least-64-character-very-long-and-secure-secret-key-for-hs512-logic-2026" \
  kapitalbank.merchant-id="E1000010" \
  kapitalbank.cert-path="/app/certificates/merchant.crt" \
  kapitalbank.key-path="/app/certificates/merchant.key"
```

---

### Init Databases SQL Addition

```sql
-- Create payment_service_db and its user
\c postgres
CREATE DATABASE payment_service_db;
CREATE USER payment_service_db WITH PASSWORD 'payment_service_db_password_12345';
GRANT ALL PRIVILEGES ON DATABASE payment_service_db TO payment_service_db;
\c payment_service_db
GRANT ALL ON SCHEMA public TO payment_service_db;
```

---

### How to Get SSL Certificates for Testing

To test with Kapital Bank, you need to:
1. Contact Kapital Bank's e-commerce department and request test merchant credentials
2. They will provide: merchant ID, SSL certificate (`.crt`), and private key (`.key`)
3. Place these files in `src/main/resources/certificates/`
4. The test merchant ID `E1000010` is commonly used in examples but you need your own

For local development without real certificates, you can create a mock `KapitalBankClient` that simulates the payment flow (always returns success after a delay).

---

### Important Notes

1. **Amount conversion:** Kapital Bank expects amounts in minor units (tiyin). 1 AZN = 100 tiyin. So 50.00 AZN = `5000`.
2. **Callback URLs must be publicly accessible** — during local development, use a tool like ngrok to expose your local server.
3. **Do NOT store full card numbers** — Kapital Bank handles all PCI-DSS compliance on their payment page.
4. **Idempotency:** Prevent duplicate payments for the same order. Check if a payment already exists before creating a new one.
5. **Timeout handling:** If the customer doesn't complete payment within a reasonable time (e.g., 15 minutes), consider implementing a scheduled job to check and expire stale INITIATED payments.
6. **The SecurityConfig must permit callback endpoints without authentication** — add `/payments/callback/**` to the public endpoints list.
