# Architecture Decision Records

## ADR-001: Manual Kafka Offset Acknowledgment

**Status:** Accepted

**Context:** In `payment-service`, processing a payment is a non-idempotent financial operation. If we use Spring Kafka's default auto-commit, a consumer crash after processing but before the offset is committed would cause the message to be redelivered — resulting in a duplicate payment charge.

**Decision:** Use `MANUAL_IMMEDIATE` acknowledgment mode. The consumer explicitly calls `acknowledgment.acknowledge()` only after the full payment processing pipeline (DB write + downstream publish) succeeds.

**Consequences:** Slightly more complex consumer code, but guarantees at-most-once charging combined with our idempotency key check.

---

## ADR-002: Idempotency via Database Unique Constraint

**Status:** Accepted

**Context:** Kafka guarantees at-least-once delivery. Payment-service must not process the same order twice.

**Decision:** The `payments` table has a `UNIQUE` constraint on `order_id`. Before processing, we check `existsByOrderId()`. If already present, we skip. If the check passes but a concurrent duplicate races in, the DB constraint will reject the insert with a unique violation — which is caught and logged as a harmless duplicate.

**Consequences:** Simple, no distributed locking needed. Works correctly under concurrent redelivery.

---

## ADR-003: Dead Letter Topic (DLT) for Failed Messages

**Status:** Accepted

**Context:** Transient failures (DB unavailable, downstream timeout) should be retried. But poison-pill messages that always fail should not block partition processing forever.

**Decision:** Use Spring Kafka's `@RetryableTopic` with 3 attempts and exponential backoff (1s, 2s, 4s). After exhausting retries, messages are routed to `<topic>.DLT`. A separate `@KafkaListener` on the DLT logs and alerts.

**Consequences:** Healthy messages are never blocked by bad ones. DLT provides an audit trail and replay capability.

---

## ADR-004: Separate PostgreSQL Instances per Service

**Status:** Accepted

**Context:** Microservices should own their data. Sharing a DB creates tight coupling, shared schema migrations, and single-point-of-failure.

**Decision:** `order-service` uses `postgres-orders:5432`. `payment-service` uses `postgres-payments:5433`. `notification-service` is stateless (no DB).

**Consequences:** Each service can scale, migrate, and evolve its schema independently. Cross-service queries require API calls or event sourcing — which is the correct approach.

---

## ADR-005: Message Key = orderId

**Status:** Accepted

**Context:** Kafka partitions messages by key. All events for the same order must land on the same partition to preserve ordering (order.created must be consumed before payment.processed for the same order).

**Decision:** Both producers use `orderId` as the Kafka message key.

**Consequences:** Events for the same order are strictly ordered within a partition. Different orders can be processed in parallel across partitions.
