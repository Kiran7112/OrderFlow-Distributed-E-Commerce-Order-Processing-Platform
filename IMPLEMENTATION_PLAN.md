# OrderFlow — Distributed E-Commerce Order Processing Platform

## Context

Build a full microservices-based e-commerce order processing system ("OrderFlow") as described in `Business_use_Case.txt`. The user has **Java only** installed locally — no Docker, Kafka, PostgreSQL, Redis, or Node.js. The complete codebase will be written locally, then **all services run on Amazon EC2** using Docker Compose. Goal: interview-ready portfolio project demonstrating Kafka event-driven architecture, Saga pattern, and microservices.

---

## Constraints & Rules (saved to memory)

- **Local:** Java 17 (JDK) only. No npm, no Docker, no Kafka, no databases.
- **Remote (EC2):** Everything else — React build/serve, Kafka, PostgreSQL, Redis, Docker, Docker Compose.
- All configs use env vars (`${KAFKA_BOOTSTRAP_SERVERS}`, `${DB_HOST}`, etc.) pointing to EC2.
- Never suggest local infra setup commands (`docker-compose up` locally, `brew install`, etc.).
- Provide Docker Compose files and EC2 shell scripts so the user can spin up everything on EC2.

---

## Project Structure

The backend is a **Maven multi-module (reactor) monorepo** — one parent POM
aggregates all services, centralizes versions, and builds everything with a
single command. Each service uses the standard layered package architecture.

```
orderflow/
├── frontend/                        # React 18 + TS (Vite) + Tailwind + Redux
│   └── src/{components,pages,services,store,types}
│
├── services/                        # Maven multi-module reactor (backend)
│   ├── pom.xml                      # ← Parent/reactor POM (modules + versions)
│   ├── .dockerignore
│   ├── api-gateway/                 # Spring Cloud Gateway — port 8080
│   ├── order-service/               # Spring Boot + PostgreSQL — port 8081
│   ├── inventory-service/           # Spring Boot + PostgreSQL + Redis — port 8082
│   ├── payment-service/             # Spring Boot + PostgreSQL — port 8083
│   ├── notification-service/        # Spring Boot (Kafka consumer only) — port 8084
│   ├── shipping-service/            # Spring Boot + PostgreSQL — port 8085
│   └── analytics-service/           # Spring Boot + PostgreSQL — port 8086
│       └── src/main/java/com/orderflow/<svc>/
│           ├── controller/  service/  repository/  entity/
│           ├── dto/  event/  kafka/  config/
│           └── <Svc>Application.java
│
├── infrastructure/
│   ├── postgres/init/               # Per-service SQL init scripts (01..05)
│   └── kafka/create-topics.sh       # Topic creation script
│
├── docker-compose.yml               # Full stack on EC2 (13 containers)
├── ec2-setup.sh                     # EC2 bootstrap (Docker + build + run)
├── Makefile                         # build / up / down / logs helpers
└── .env.example                     # EC2 environment variable template
```

### Build model (industry standard)

- **Parent POM** `services/pom.xml` (`packaging=pom`) inherits
  `spring-boot-starter-parent`, declares all 7 modules, and centralizes
  dependency versions in `<dependencyManagement>` + `<properties>`.
- **Child POMs** inherit `orderflow-parent` via `<relativePath>../pom.xml</relativePath>`
  — no duplicated version numbers.
- **Build all:** `cd services && mvn clean package` · **one module:**
  `mvn -pl order-service -am clean package`.
- **Docker:** build context is `./services`; each `Dockerfile` runs
  `mvn -pl <module> -am package` so the parent POM resolves inside the image.
- **Bounded contexts:** event/DTO classes are intentionally **owned per service**
  (no shared-kernel module) to keep services loosely coupled — a deliberate
  microservices design choice.

---

## Implementation Status

| Phase | Description | Status |
|---|---|---|
| 1 | Infrastructure & API Gateway | ✅ Complete |
| 2 | Order Service (Kafka Producer) | ✅ Complete |
| 3 | Inventory Service (Redis + Saga) | ✅ Complete |
| 4 | Payment Service (Mock Gateway + Saga) | ✅ Complete |
| 5 | Shipping Service (Delivery Simulator) | ✅ Complete |
| 6 | Notification Service (Async, No DB) | ✅ Complete |
| 7 | Analytics Service (Metrics + Consumer Lag) | ✅ Complete |
| 8 | React Frontend (3 Portals) | ✅ Complete |
| 9 | EC2 Deployment & Kafka Topics | ✅ Complete |

**All phases complete.** See `DEPLOYMENT.md` for EC2 deployment + end-to-end verification.

---

## Implementation Phases

### Phase 1 — Infrastructure & API Gateway ✅

**Files to create:**

1. **`docker-compose.yml`** — defines all services:
   - Zookeeper + Kafka (bitnami/kafka:3.6)
   - PostgreSQL 15 (one instance, multiple databases: orders_db, inventory_db, payments_db, shipping_db, analytics_db)
   - Redis 7
   - All 7 Spring Boot services (built from local JARs)
   - React frontend (nginx serving the build)
   - All connected via `orderflow-network` bridge

2. **`.env.example`** — template with:
   ```
   EC2_HOST=<your-ec2-ip>
   KAFKA_BOOTSTRAP_SERVERS=kafka:9092
   DB_HOST=postgres
   DB_PORT=5432
   REDIS_HOST=redis
   JWT_SECRET=<secret>
   ```

3. **`ec2-setup.sh`** — installs Docker, Docker Compose, clones repo, runs `docker-compose up`

4. **`services/api-gateway/`** — Spring Cloud Gateway:
   - `pom.xml`: `spring-cloud-starter-gateway`, `spring-security`, `jjwt`
   - `application.yml`: routes to all 6 backend services, JWT filter, rate limiter
   - `JwtAuthFilter.java`: validates Bearer token on every request
   - `RouteConfig.java`: route definitions

5. **`infrastructure/postgres/init/`** — SQL files:
   - `01_orders_db.sql`
   - `02_inventory_db.sql`
   - `03_payments_db.sql`
   - `04_shipping_db.sql`
   - `05_analytics_db.sql`

---

### Phase 2 — Order Service (Kafka Producer) ✅

**`services/order-service/`**

- `pom.xml`: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-kafka`, `postgresql`, `lombok`
- `application.yml`: DB config via `${DB_HOST}`, Kafka config via `${KAFKA_BOOTSTRAP_SERVERS}`
- **Entities:** `Order.java`, `OrderItem.java` (status enum: PLACED/CONFIRMED/SHIPPED/DELIVERED/CANCELLED/PAYMENT_FAILED)
- **Repository:** `OrderRepository.java`
- **Kafka Producer:** `OrderEventPublisher.java` — publishes `OrderPlacedEvent` to `order-events` topic
- **Service:** `OrderService.java` — creates order, saves to DB, publishes event
- **Controller:** `OrderController.java`
  - `POST /api/orders` — place order
  - `GET /api/orders/{id}` — get order + status
  - `GET /api/orders/customer/{customerId}` — order history
  - `PATCH /api/orders/{id}/cancel` — cancel if before SHIPPED
- **Kafka Consumer:** `OrderStatusUpdateConsumer.java` — listens to `inventory-events` and `payment-events` to update order status (Saga compensations)
- **DTOs:** `OrderRequest.java`, `OrderResponse.java`, `OrderPlacedEvent.java`

---

### Phase 3 — Inventory Service (Redis + Saga) ✅

**`services/inventory-service/`**

- `pom.xml`: adds `spring-boot-starter-data-redis`
- **Entities:** `Product.java`, `Stock.java`
- **Redis Config:** `RedisConfig.java` — connects to `${REDIS_HOST}`, TTL 30s for stock counts
- **Kafka Consumer:** `OrderPlacedConsumer.java`
  - Listens to `order-events`
  - Checks Redis cache first, falls back to DB
  - Uses `@Transactional` + optimistic locking on `Stock` to reserve units
  - Publishes `InventoryReservedEvent` or `InventoryInsufficientEvent` to `inventory-events`
- **Saga Compensation Consumer:** `PaymentFailedCompensationConsumer.java`
  - Listens to `payment-events` for `payment.failed`
  - Releases stock reservation
- **Controller:** `InventoryController.java`
  - `GET /api/inventory/{productId}`
  - `PUT /api/inventory/{productId}` (admin)
  - `GET /api/inventory/low-stock`
- **Idempotency:** `ProcessedEventRepository.java` — stores processed event IDs to prevent duplicate processing

---

### Phase 4 — Payment Service (Mock Gateway + Saga) ✅

**`services/payment-service/`**

- **Entity:** `Transaction.java` (status: PENDING/SUCCESS/FAILED)
- **Kafka Consumer:** `InventoryReservedConsumer.java`
  - Listens to `inventory-events` for `inventory.reserved`
  - Calls `MockPaymentGateway.java` (simulates 90% success rate, random delay)
  - Saves transaction to `payments_db`
  - Publishes `PaymentSuccessEvent` or `PaymentFailedEvent` to `payment-events`
- **Mock Gateway:** `MockPaymentGateway.java` — simulates Stripe/Razorpay response
- **Controller:** `PaymentController.java`
  - `GET /api/payments/order/{orderId}` — get payment status
- **Idempotency:** checks `order_id` uniqueness before processing

---

### Phase 5 — Shipping Service ✅

**`services/shipping-service/`**

- **Entity:** `Shipment.java` (status: CREATED/IN_TRANSIT/DELIVERED)
- **Kafka Consumer:** `PaymentSuccessConsumer.java`
  - Listens to `payment-events` for `payment.success`
  - Creates shipment, generates tracking ID (`TRK-{year}-{random5digits}`)
  - Publishes `ShipmentCreatedEvent` to `shipping-events`
  - Schedules `ShipmentDeliverySimulator.java` (configurable delay via `${DELIVERY_DELAY_SECONDS}`)
  - Publishes `ShipmentDeliveredEvent` after delay
- **Controller:** `ShippingController.java`
  - `GET /api/shipping/order/{orderId}` — tracking info

---

### Phase 6 — Notification Service (Async, No DB) ✅

**`services/notification-service/`**

- No database — stateless consumer
- **Kafka Consumers** (separate consumer groups):
  - `OrderEventNotificationConsumer.java` — listens `order-events`
  - `PaymentEventNotificationConsumer.java` — listens `payment-events`
  - `ShippingEventNotificationConsumer.java` — listens `shipping-events`
- **NotificationService.java** — logs email content to console (mock; real SMTP via `${SMTP_HOST}` if configured)
- Templates for: Order Placed, Payment Success, Payment Failed, Shipped, Delivered, Out of Stock

---

### Phase 7 — Analytics Service ✅

**`services/analytics-service/`**

- **Kafka Consumer:** `AllEventsConsumer.java` — subscribes to all 4 topics, persists to `analytics_db`
- **Entities:** `OrderMetric.java`, `RevenueRecord.java`, `FailureEvent.java`
- **Controller:** `AnalyticsController.java`
  - `GET /api/analytics/orders/summary` — orders per hour today
  - `GET /api/analytics/revenue/daily` — last 30 days
  - `GET /api/analytics/failures` — failure rates by type
  - `GET /api/analytics/kafka/consumer-lag` — queries Kafka Admin API for consumer lag
- **KafkaAdminService.java** — wraps `AdminClient` to fetch consumer group offsets

---

### Phase 8 — React Frontend ✅

**`frontend/`** (runs on EC2, built with Node.js on EC2)

- `package.json`: React 18, TypeScript, Tailwind CSS, shadcn/ui, Redux Toolkit, RTK Query, Recharts, React Router v6
- **Pages:**
  - `CustomerPortal/ProductListing.tsx` — product grid with live stock badge
  - `CustomerPortal/Cart.tsx` — cart with totals
  - `CustomerPortal/Checkout.tsx` — address + payment form → calls Order Service
  - `CustomerPortal/OrderTracking.tsx` — polls `/api/orders/{id}` every 3s, status timeline
  - `CustomerPortal/OrderHistory.tsx` — past orders list
  - `OperationsDashboard/LivePipeline.tsx` — order counts per stage (SSE from Analytics)
  - `OperationsDashboard/RevenueChart.tsx` — Recharts line chart (daily revenue)
  - `OperationsDashboard/FailureMonitor.tsx` — failure rate cards
  - `OperationsDashboard/KafkaLagPanel.tsx` — consumer lag per group
  - `OperationsDashboard/ServiceHealth.tsx` — pings Spring Actuator `/actuator/health`
  - `InventoryPortal/StockDashboard.tsx` — low stock alerts
  - `InventoryPortal/ProductCRUD.tsx` — add/edit products
- **Redux store:** `store/` — auth slice, cart slice
- **RTK Query:** `services/api.ts` — all API calls via `REACT_APP_API_URL` env var (points to EC2 API Gateway)
- **Auth:** JWT stored in localStorage, attached as Bearer header

---

### Phase 9 — EC2 Deployment Files ✅

1. **`ec2-setup.sh`**:
   ```bash
   #!/bin/bash
   sudo apt-get update
   sudo apt-get install -y docker.io docker-compose git
   sudo systemctl start docker
   sudo usermod -aG docker ubuntu
   git clone <repo-url> /home/ubuntu/orderflow
   cd /home/ubuntu/orderflow
   cp .env.example .env
   # User fills in .env with their EC2 IP and secrets
   docker-compose up -d --build
   ```

2. **`docker-compose.yml`** — health checks on all services, restart: always, depends_on with condition: service_healthy

3. **`Makefile`** (optional helper):
   - `make build` — builds all JARs locally with `mvn package -DskipTests`
   - `make deploy` — SCPs JARs to EC2 and restarts containers

---

## Kafka Topic Configuration

Topics created by `infrastructure/kafka/create-topics.sh` (runs as Docker init container):
| Topic | Partitions | Replication |
|---|---|---|
| `order-events` | 6 | 1 (EC2 single-node) |
| `inventory-events` | 6 | 1 |
| `payment-events` | 6 | 1 |
| `shipping-events` | 6 | 1 |
| `dlq-events` | 3 | 1 |

> Replication factor 1 for EC2 single-broker dev setup (use 3 for production multi-broker).

---

## Saga Pattern Summary

| Failure Scenario | Compensation |
|---|---|
| Inventory insufficient | `OrderStatusUpdateConsumer` marks order CANCELLED |
| Payment failed | `PaymentFailedCompensationConsumer` releases stock; order → PAYMENT_FAILED |
| Service crash | Kafka retains message; service reprocesses on restart (idempotency key prevents duplicate) |

---

## Security

- JWT issued by API Gateway on `/auth/login`
- All downstream services validate JWT via shared `${JWT_SECRET}` env var
- Admin-only endpoints (inventory PUT) checked via role claim in JWT

---

## Verification (on EC2)

1. SSH into EC2, run `docker-compose ps` — all 13 containers healthy
2. `curl http://EC2_IP:8080/api/orders -H "Authorization: Bearer <token>"` — returns 200
3. Place order via React UI → watch order status progress in OrderTracking page
4. Introduce payment failure (mock returns fail) → verify stock is released and order shows PAYMENT_FAILED
5. Check Analytics dashboard shows updated metrics
6. Check Kafka consumer lag panel shows 0 lag (all events consumed)

---

## Build Order (implementation sequence)

1. `docker-compose.yml` + `.env.example` + `ec2-setup.sh`
2. DB init SQL scripts
3. `api-gateway` service
4. `order-service`
5. `inventory-service`
6. `payment-service`
7. `shipping-service`
8. `notification-service`
9. `analytics-service`
10. React `frontend`
