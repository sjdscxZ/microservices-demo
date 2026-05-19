# STUDY notes — microservices-demo

Interview-prep companion. Read this **before** sending this repo URL on a resume. The interviewer will probe specific design choices — be ready to answer the ones below in your own words.

## Headline claims to own

1. "I built an event-driven microservices system with Spring Boot, Kafka, and Spring Cloud Gateway."
2. "User and order services publish domain events; notification service consumes them."
3. "Prometheus scrapes per-service metrics; Grafana dashboards segment by `application` tag."

## Likely interview questions + how to answer

### Q1. "Why event-driven instead of REST between services?"
- **Decoupling**: order-service doesn't need to know notification-service exists.
- **Resilience**: if notification-service is down, the event sits in the topic; once it's back up it consumes. With sync REST, the order call would fail.
- **Throughput**: the order endpoint returns 201 as soon as the event is sent — no waiting on downstream work.
- **Replay**: you can rewind a Kafka consumer group offset to reprocess history (e.g., resending lost notifications after a bug fix).

### Q2. "How does the order service know about new users?"
The `UserCreatedListener` in `order-service` subscribes to `user.created` with `groupId=order-service`. Kafka guarantees each consumer group sees every message at least once. If we add another instance of order-service, Kafka rebalances partitions across them — both instances share the load, no duplicate processing.

### Q3. "What about delivery guarantees?"
- Producer: `acks=all`, `retries=3` (see `application.yml`). Survives single-broker loss.
- Consumer: `auto-offset-reset=earliest`. New consumers replay from beginning of topic. Production would also set `enable.auto.commit=false` and commit offsets manually after successful processing for **at-least-once** semantics.
- Exactly-once would need transactional producer + idempotent consumer; out of scope here.

### Q4. "Why a gateway? Why not let clients hit services directly?"
- One URL to publish; backend services can change ports/hosts.
- Central place for cross-cutting concerns: CORS, rate limiting, JWT validation, logging.
- Easier security posture — only the gateway is exposed; services live behind a private network.

### Q5. "How do you observe what's happening?"
- Each service exposes `/actuator/prometheus` via `micrometer-registry-prometheus`.
- Prometheus scrapes every 15s (see `infra/prometheus/prometheus.yml`).
- Grafana data source is provisioned at startup from `infra/grafana/provisioning/`.
- Custom metric example: `notifications_delivered_total` counter in `notification-service`, incremented per processed event.

### Q6. "What's missing that you'd add in prod?"
- **Distributed tracing** — OpenTelemetry agent → Jaeger/Tempo. Currently I have logs and metrics but no trace correlation across services.
- **Schema registry** (Confluent or Apicurio) for Avro events instead of raw JSON strings. Today producer/consumer drift would silently break things.
- **Dead-letter topics** for poison messages.
- **Outbox pattern** — write the event to a DB table in the same transaction as the order, then a relay publishes to Kafka. Avoids the "wrote to DB but Kafka publish failed" inconsistency.
- **K8s** — currently Docker Compose. K8s gives us autoscaling, rolling restarts, pod-level health.

### Q7. "Show me how a JWT would propagate through this."
- Gateway validates JWT (Bearer header) using a public key.
- Gateway forwards the token in `Authorization` header to downstream services.
- Each service has `SecurityFilterChain` (see `user-service/SecurityConfig.java`) that decodes the JWT and binds claims to `SecurityContext`.
- For this demo, security is currently `permitAll` on the user endpoints to keep curl examples simple — the **infrastructure** is in place but routes are not yet locked down.

### Q8. "How would you test the Kafka flow end-to-end?"
- Already done: `OrderControllerTest` uses `@EmbeddedKafka` to spin up an in-process broker per test. The test publishes via the controller; a hand-rolled consumer (`KafkaTestConsumer`, not in this repo yet) could subscribe and assert the message.
- Production CI: spin up real Kafka with Testcontainers (`testcontainers-kafka`) instead of embedded; closer to real broker behavior.

## Files to be able to navigate from memory

- `pom.xml` — parent BOM, modules list, Spring Boot version
- `user-service/src/main/java/com/sjdscxz/userservice/UserController.java` — Kafka producer call
- `notification-service/.../NotificationServiceApplication.java` — `@KafkaListener` + Micrometer counter
- `api-gateway/src/main/resources/application.yml` — route table
- `docker-compose.yml` — service graph
- `infra/prometheus/prometheus.yml` — scrape jobs

## Honest gaps (own these in the interview)

- No K8s manifests yet — only Docker Compose.
- No real OAuth server — security infra is wired but unsecured for demo.
- No GraphQL — REST only.
- No Terraform — local Compose only.
- These are listed in the README roadmap; they're aspirational, not delivered.

## Behavioral framing

If asked **"What was hard about this?"**:
- *"Getting Spring Cloud Gateway's reactive stack to play nicely with the rest of the Spring MVC services was the first surprise — gateway uses WebFlux, which doesn't auto-configure JPA. Solved it by keeping gateway dependency-light."*
- *"Designing for `at-least-once` consumer semantics — what does the notification service do if it receives the same `order.placed` event twice? Idempotency via the order ID."*

If asked **"What would you do differently?"**:
- *"Reach for the outbox pattern earlier. Right now the order service writes to JPA and publishes to Kafka in the same controller method — if Kafka is briefly down between those two calls, we've created an order with no event. An outbox table inside the order DB, drained by a separate publisher, removes that window."*
