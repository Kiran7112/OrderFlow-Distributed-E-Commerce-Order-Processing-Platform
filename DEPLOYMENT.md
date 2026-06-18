# OrderFlow — EC2 Deployment Guide

This guide walks you through deploying the full OrderFlow stack on a single Amazon EC2 instance using Docker Compose.

> **Local machine:** You only need Java to build/inspect the code. Everything else (Docker, Kafka, PostgreSQL, Redis, the React build) runs on EC2.

> **Build model:** The backend is a **Maven multi-module reactor**. Each service's
> Docker image is built from the `services/` context with
> `mvn -pl <module> -am package`, so the parent POM (`services/pom.xml`) is
> available during the build. You don't run this by hand —
> `ec2-setup.sh` → `docker-compose up --build` does it for you.

---

## 1. Provision the EC2 Instance

| Setting | Recommendation |
|---|---|
| Instance type | `t3.large` minimum (2 vCPU, 8 GB RAM) — the full stack runs ~13 containers |
| OS | Ubuntu 22.04 LTS |
| Storage | 30 GB gp3 |
| Security group | See ports below |

### Required inbound ports (Security Group)

| Port | Purpose |
|---|---|
| 22 | SSH |
| 3000 | React frontend |
| 8080 | API Gateway |
| 8081–8086 | (optional) direct service access for debugging |

> For production, expose only 3000 and 8080 publicly; keep service ports internal.

---

## 2. Copy the Project to EC2

From your local machine:

```bash
# Option A — using the Makefile helper
make deploy-ec2 EC2_HOST=<your-ec2-public-ip>

# Option B — manual copy
scp -i your-key.pem -r . ubuntu@<your-ec2-public-ip>:/home/ubuntu/orderflow
```

---

## 3. Run the Setup Script

SSH into the instance and bootstrap Docker + the stack:

```bash
ssh -i your-key.pem ubuntu@<your-ec2-public-ip>
cd /home/ubuntu/orderflow

# Configure environment
cp .env.example .env
nano .env          # set EC2_HOST=<your-ec2-public-ip>, DB_PASSWORD, JWT_SECRET

# Run bootstrap (installs Docker, Docker Compose, builds & starts everything)
bash ec2-setup.sh
```

> Re-log after the script adds you to the `docker` group, or use `sudo docker-compose` for the first run.

---

## 4. Verify Containers Are Healthy

```bash
docker-compose ps
```

Expect these containers, all `healthy` or `running`:

```
zookeeper            healthy
kafka                healthy
kafka-init           exited (0)     <- creates topics, then exits — this is expected
postgres             healthy
redis                healthy
api-gateway          healthy
order-service        healthy
inventory-service    healthy
payment-service      healthy
notification-service healthy
shipping-service     healthy
analytics-service    healthy
frontend             healthy
```

Confirm the Kafka topics were created:

```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
# order-events, inventory-events, payment-events, shipping-events, dlq-events
```

Confirm the databases exist:

```bash
docker-compose exec postgres psql -U postgres -c "\l"
# orders_db, inventory_db, payments_db, shipping_db, analytics_db
```

---

## 5. End-to-End Verification (the Saga in action)

### 5.1 Open the app

- Frontend: `http://<your-ec2-public-ip>:3000`
- API Gateway health: `http://<your-ec2-public-ip>:8080/actuator/health`

### 5.2 Place an order (happy path)

From the UI: **Shop → Add to Cart → Cart → Checkout → Place Order**, or via curl:

```bash
curl -X POST http://<your-ec2-public-ip>:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "99999999-9999-9999-9999-999999999999",
    "items": [
      { "productId": "22222222-2222-2222-2222-222222222222", "quantity": 1, "unitPrice": 29.99 }
    ]
  }'
```

Note the returned `id` — that's your `orderId`.

### 5.3 Watch the saga progress

On the **Order Tracking** page (`/track/:orderId`) the status timeline auto-polls every 3s and advances:

```
PLACED  →  CONFIRMED  →  SHIPPED  →  DELIVERED
```

Behind the scenes:
1. Order Service publishes `order.placed`
2. Inventory Service reserves stock → `inventory.reserved` (status → CONFIRMED)
3. Payment Service processes payment → `payment.success` (status → SHIPPED)
4. Shipping Service creates a shipment, assigns `TRK-YYYY-NNNNN`, then after `DELIVERY_DELAY_SECONDS` → `shipment.delivered` (status → DELIVERED)
5. Notification Service logs an email at each step
6. Analytics Service aggregates every event

Watch the events flow live:

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

Watch notification emails (console-logged):

```bash
docker-compose logs -f notification-service
```

### 5.4 Trigger a failure (compensation path)

The mock payment gateway fails ~10% of the time. Place several orders until one fails; that order's status becomes `PAYMENT_FAILED` and the Inventory Service releases the reservation (Saga compensation). Out-of-stock products produce `CANCELLED`.

### 5.5 Check analytics & monitoring

- **Operations Dashboard** (`/ops`): live pipeline counts, revenue chart, failure cards, Kafka consumer lag, service-health grid.
- Direct API checks:

```bash
curl http://<your-ec2-public-ip>:8080/api/analytics/orders/summary
curl http://<your-ec2-public-ip>:8080/api/analytics/revenue/daily
curl http://<your-ec2-public-ip>:8080/api/analytics/failures
curl http://<your-ec2-public-ip>:8080/api/analytics/kafka/consumer-lag
```

Consumer lag should trend toward **0** once all consumers catch up.

---

## 6. Operations Cheatsheet

```bash
docker-compose ps                       # status
docker-compose logs -f <service>        # follow a service's logs
docker-compose restart <service>        # restart one service
docker-compose down                     # stop everything (keeps volumes)
docker-compose down -v                  # stop + wipe data volumes
docker-compose up -d --build            # rebuild & restart
```

### Reseed / reset

```bash
docker-compose down -v        # drops postgres-data, kafka-data, redis-data
docker-compose up -d --build  # re-runs init SQL + topic creation
```

---

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| `kafka-init` keeps restarting | It shouldn't — it has `restart: "no"`. Check `docker-compose logs kafka-init`; ensure Kafka is healthy first. |
| Service can't reach DB | `docker-compose logs <service>`; confirm `postgres` is healthy and the per-service DB exists. |
| Consumer lag growing | A consumer is down or erroring. Check that service's logs; restart it. |
| Frontend can't reach API | Ensure `.env` `EC2_HOST` is your public IP and you rebuilt the frontend (`docker-compose up -d --build frontend`). The API URL is baked in at build time. |
| Out of memory | Use a larger instance (`t3.large`+). The JVM services + Kafka are memory-hungry. |

---

## 8. Production Hardening (next steps)

- Move Kafka to **Amazon MSK**, PostgreSQL to **RDS**, Redis to **ElastiCache**
- Put an **ALB** in front of the API Gateway with TLS
- Store `JWT_SECRET` / DB creds in **AWS Secrets Manager**
- Add **Prometheus + Grafana** (Actuator endpoints already exposed)
- Set replication factor to 3 across a multi-broker cluster
- Add CI/CD (GitHub Actions / CodePipeline)
