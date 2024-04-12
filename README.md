# Event-Driven Order Processing System

A production-grade microservices system demonstrating event-driven architecture using Apache Kafka, Spring Boot 3, and Docker Compose.

## Architecture

```
[Client] → POST /api/orders
              │
         [order-service]  ──── Kafka: order.created ────►  [payment-service]
              │                                                     │
              │                                          Kafka: payment.processed
              │                                                     │
              └─────────────────────────────────────────► [notification-service]
```

### Services

| Service | Port | Responsibility |
|---|---|---|
| `order-service` | 8081 | Accepts orders, persists to PostgreSQL, publishes `order.created` |
| `payment-service` | 8082 | Consumes `order.created`, processes payment, publishes `payment.processed` |
| `notification-service` | 8083 | Consumes both events, logs/sends notifications |

### Infrastructure

| Component | Port | Purpose |
|---|---|---|
| Apache Kafka | 9092 | Message broker |
| Zookeeper | 2181 | Kafka coordination |
| PostgreSQL (orders) | 5432 | Order persistence |
| PostgreSQL (payments) | 5433 | Payment persistence |
| Prometheus | 9090 | Metrics scraping |
| Grafana | 3000 | Metrics dashboards |
| Kafka UI | 8080 | Kafka topic browser |

## Key Design Decisions

- **Dead Letter Queue (DLQ)**: Failed messages routed to `order.created.DLT` and `payment.processed.DLT`
- **Idempotency**: Payment service deduplicates by `orderId` to handle Kafka redelivery
- **Outbox Pattern**: Order service writes to DB and publishes in the same transaction boundary
- **Retry logic**: Exponential backoff (3 retries) before DLQ routing
- **Observability**: Micrometer + Prometheus + Grafana for all services

## Running Locally

```bash
# Start all infrastructure + services
docker-compose up -d

# Check all services are healthy
docker-compose ps

# Place a test order
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-123","items":[{"productId":"prod-456","quantity":2,"price":49.99}]}'

# View Kafka topics
open http://localhost:8080

# View metrics
open http://localhost:9090   # Prometheus
open http://localhost:3000   # Grafana (admin/admin)
```

## Running Tests

```bash
# Each service has unit + integration tests
cd order-service && ./mvnw test
cd payment-service && ./mvnw test
cd notification-service && ./mvnw test
```

## Tech Stack

- **Java 21** + **Spring Boot 3.2**
- **Apache Kafka** with Spring Kafka
- **PostgreSQL** with Spring Data JPA
- **Docker Compose** for local orchestration
- **Prometheus + Grafana** for observability
- **JUnit 5 + Mockito + Testcontainers** for testing
