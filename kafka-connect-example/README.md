# Kafka Connect POC: Order Service → Analytics (DB + S3)

A proof-of-concept demonstrating **Kafka Connect** with a real use case: orders flow from an **order-service** into a database, then a **source connector** streams changes to Kafka; two **sink connectors** persist the same stream to **PostgreSQL** (analytics DB) and **S3 (MinIO)**. An **analytics-service** reads from both stores.

Kafka runs in **KRaft mode** (Apache Kafka 3.8.1)—no Zookeeper.

## Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────────────────────────────┐
│  order-service  │────▶│  orders-db   │     │           Kafka Connect                  │
│  (REST API)     │     │  (Postgres)  │────▶│  Source: Debezium PostgreSQL             │
└─────────────────┘     └──────────────┘     │  → Topic: orders-server.public.orders   │
                                              │  Sink 1: JDBC → analytics-db             │
                                              │  Sink 2: S3   → MinIO (orders-events)    │
                                              └──────────────────┬──────────────────────┘
                                                                 │
                    ┌────────────────────────────────────────────┼────────────────────────┐
                    ▼                                            ▼                        │
            ┌───────────────┐                            ┌─────────────────┐              │
            │ analytics-db  │                            │ MinIO (S3)       │              │
            │ order_events  │                            │ bucket: orders-  │              │
            └───────┬───────┘                            │ events           │              │
                    │                                    └────────┬────────┘              │
                    │                                             │                       │
                    └─────────────────────┬───────────────────────┘                       │
                                         ▼                                                 │
                              ┌─────────────────────┐                                     │
                              │  analytics-service  │◀────────────────────────────────────┘
                              │  - /orders-from-db  │   (reads from both SQL and S3)
                              │  - /orders-from-s3  │
                              └─────────────────────┘
```

## Prerequisites

- **Docker** and **Docker Compose**
- **Git** (optional, for cloning)

## Quick Start

1. **Start everything** (from the project root):

   ```bash
   docker compose up -d --build
   ```

2. **Wait for services** (Kafka Connect can take 1–2 minutes to become healthy). Optional: run connector registration manually if you didn’t rely on `connector-init`:

   ```bash
   chmod +x scripts/register-connectors.sh
   ./scripts/register-connectors.sh
   ```

3. **Create some orders** (order-service writes to `orders-db`):

   ```bash
   curl -X POST http://localhost:8080/api/orders \
     -H "Content-Type: application/json" \
     -d '{"customerId":"cust-1","customerEmail":"a@b.com","totalAmount":99.99}'
   ```

4. **Verify the pipeline**  
   - Debezium reads from `orders-db` and publishes to `orders-server.public.orders`.  
   - JDBC sink writes into `analytics-db.order_events`.  
   - S3 sink writes JSON into MinIO bucket `orders-events`.

5. **Query analytics**  
   - From **analytics DB** (replicated via JDBC sink):  
     `GET http://localhost:8081/api/analytics/orders-from-db`  
     `GET http://localhost:8081/api/analytics/orders-from-db/summary`  
   - From **S3** (MinIO):  
     `GET http://localhost:8081/api/analytics/orders-from-s3/summary`  
     `GET http://localhost:8081/api/analytics/orders-from-s3/keys`

## Services & Ports

| Service          | Port(s)      | Description                                      |
|------------------|--------------|--------------------------------------------------|
| order-service    | 8080         | REST API; persists orders to `orders-db`         |
| analytics-service| 8081         | Reads from analytics-db and MinIO                 |
| Kafka (KRaft)    | 9092, 29092, 9093 | Broker; no Zookeeper (Apache Kafka 3.8.1)    |
| Kafka Connect    | 8083         | REST API for connector management                 |
| orders-db        | 5432         | PostgreSQL (order-service + Debezium source)     |
| analytics-db      | 5433 (host)  | PostgreSQL (JDBC sink target)                     |
| MinIO            | 9000, 9001   | S3-compatible storage (S3 sink target); 9001 UI  |

**One-shot containers** (exiting is normal): `minio-init` creates the MinIO bucket then exits; `connector-init` registers the Kafka Connect connectors then exits. Both use `restart: "no"`.

## Connectors

- **orders-db-source** (Debezium PostgreSQL): tails `public.orders` in `orders-db`, publishes to `orders-server.public.orders`.
- **orders-to-analytics-jdbc-sink**: consumes that topic and upserts into `analytics-db.order_events`.
- **orders-to-s3-sink**: consumes the same topic and writes JSON files into MinIO bucket `orders-events`.

Configs are in `connectors/`. Register via:

- Automatic: `connector-init` container runs after Kafka Connect is healthy.
- Manual:  
  `curl -X POST -H "Content-Type: application/json" --data @connectors/debezium-orders-source.json http://localhost:8083/connectors`  
  (and similarly for the two sink connectors).

## Useful Commands

```bash
# List connectors
curl -s http://localhost:8083/connectors | jq

# Connector status
curl -s http://localhost:8083/connectors/orders-db-source/status | jq

# MinIO console (browser)
open http://localhost:9001   # login: minioadmin / minioadmin
```

### MinIO: where to find S3 sink files

Objects are **not** at the bucket root. In the MinIO console (http://localhost:9001):

1. Open bucket **orders-events**.
2. Open the **topics** folder.
3. Open **orders-server.public.orders**.
4. Open **partition=0** (or the partition folder).  
   The JSON files are here (e.g. `000000000000.json`).

If you see no objects:

- **Check S3 sink status:**  
  `curl -s http://localhost:8083/connectors/orders-to-s3-sink/status | jq`  
  Task should be **RUNNING**. If **FAILED**, fix the error in the trace, then update the connector config and restart.
- **Apply connector config changes** (after editing `connectors/s3-orders-sink.json`):  
  `curl -s -X PUT -H "Content-Type: application/json" -d "$(jq '.config' connectors/s3-orders-sink.json)" http://localhost:8083/connectors/orders-to-s3-sink/config`  
  then:  
  `curl -s -X POST http://localhost:8083/connectors/orders-to-s3-sink/restart`
- **Create at least one order** so the topic has messages; with `flush.size=1`, one record should produce one file.
- **List keys via analytics-service:**  
  `curl -s http://localhost:8081/api/analytics/orders-from-s3/keys | jq`  
  If this returns keys, MinIO has data; use the same path in the console (e.g. `topics/orders-server.public.orders/partition=0/...`).

## Project Layout

- `order-service/`        – Spring Boot app; orders API and JPA to `orders-db`
- `analytics-service/`    – Spring Boot app; reads from analytics-db and MinIO
- `kafka-connect/`        – Dockerfile for Connect with Debezium, JDBC, S3 plugins
- `connectors/`           – JSON configs for source and sink connectors
- `scripts/`              – DB init SQL and connector registration scripts
- `docker-compose.yml`    – All services (Kafka, Connect, DBs, MinIO, apps)

## Notes

- **PostgreSQL** for `orders-db` is started with `wal_level=logical` and a publication so Debezium can stream changes.
- **MinIO** is used as an S3-compatible store; the S3 sink uses `store.url=http://minio:9000` and the same bucket name.
- **analytics-db** `order_events.created_at` is TEXT so the JDBC sink can write Debezium’s ISO-8601 timestamp strings; use `created_at::timestamptz` in SQL for date filtering.
- If you already have `order_events` with `created_at` as `TIMESTAMP WITH TIME ZONE`, run:  
  `ALTER TABLE order_events ALTER COLUMN created_at TYPE TEXT;`  
  then restart the JDBC sink connector.
- For a **clean run**, use fresh volumes or `docker compose down -v` before `docker compose up -d --build`.
