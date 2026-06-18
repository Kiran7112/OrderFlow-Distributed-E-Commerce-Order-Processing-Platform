# OrderFlow Frontend

React 18 + TypeScript + Vite + Tailwind + Redux Toolkit (RTK Query) + Recharts.

> **Note:** Node.js is not required locally — the frontend builds and runs on EC2 via Docker (multi-stage build with nginx). See the root `README.md` and `ec2-setup.sh`.

## Portals

- **Customer Portal** — Product listing (live stock badges), Cart, Checkout, Order Tracking (3s polling timeline), Order History
- **Operations Dashboard** — Live order pipeline, Revenue chart (Recharts), Failure monitor, Kafka consumer lag, Service health grid
- **Inventory Portal** — Low stock alerts, Product stock management (CRUD)

## Configuration

All API calls go through the API Gateway via the `VITE_API_URL` env var (build-time):

```bash
cp .env.example .env
# Set VITE_API_URL=http://<your-ec2-ip>:8080
```

## Routes

| Path | Page |
|---|---|
| `/` | Product Listing |
| `/cart` | Cart |
| `/checkout` | Checkout |
| `/track/:orderId` | Order Tracking |
| `/orders` | Order History |
| `/ops` | Operations Dashboard |
| `/inventory` | Inventory Portal |

## Local commands (if Node is available)

```bash
npm install
npm run dev      # dev server on :3000
npm run build    # production build to dist/
```

## Docker (the EC2 path)

Built automatically by the root `docker-compose.yml`:

```bash
docker-compose up -d --build frontend
# Served by nginx on port 3000
```
