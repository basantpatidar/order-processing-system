 make up.PHONY: up down logs test clean build place-order

# ── Docker ─────────────────────────────────────────────────────────────────────

up:
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 10
	@docker-compose ps

down:
	docker-compose down -v

restart:
	docker-compose down -v && docker-compose up -d

logs:
	docker-compose logs -f order-service payment-service notification-service

logs-kafka:
	docker-compose logs -f kafka

# ── Testing ────────────────────────────────────────────────────────────────────

test:
	cd order-service && ./mvnw test
	cd payment-service && ./mvnw test
	cd notification-service && ./mvnw test

test-order:
	cd order-service && ./mvnw test

test-payment:
	cd payment-service && ./mvnw test

# ── Build ──────────────────────────────────────────────────────────────────────

build:
	cd order-service && ./mvnw clean package -DskipTests
	cd payment-service && ./mvnw clean package -DskipTests
	cd notification-service && ./mvnw clean package -DskipTests

# ── Demo ───────────────────────────────────────────────────────────────────────

place-order:
	curl -s -X POST http://localhost:8081/api/orders \
	  -H "Content-Type: application/json" \
	  -d '{ \
	    "customerId": "cust-demo-123", \
	    "items": [ \
	      {"productId": "prod-001", "quantity": 2, "price": 49.99}, \
	      {"productId": "prod-002", "quantity": 1, "price": 19.99} \
	    ] \
	  }' | python3 -m json.tool

health:
	@echo "=== order-service ==="
	@curl -s http://localhost:8081/actuator/health | python3 -m json.tool
	@echo "\n=== payment-service ==="
	@curl -s http://localhost:8082/actuator/health | python3 -m json.tool
	@echo "\n=== notification-service ==="
	@curl -s http://localhost:8083/actuator/health | python3 -m json.tool

clean:
	docker-compose down -v
	cd order-service && ./mvnw clean
	cd payment-service && ./mvnw clean
	cd notification-service && ./mvnw clean
