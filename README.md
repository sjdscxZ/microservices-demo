# microservices-demo

Event-driven Spring Boot microservices reference implementation. Three services communicate asynchronously via Kafka behind a Spring Cloud Gateway, with Prometheus + Grafana for observability.

## Architecture

```
                 ┌──────────────┐
   client ────▶  │ api-gateway  │   (Spring Cloud Gateway · :8080)
                 └──────┬───────┘
                        │ HTTP
       ┌────────────────┼────────────────┐
       ▼                ▼                ▼
 ┌───────────┐  ┌─────────────┐  ┌──────────────────┐
 │  user     │  │   order     │  │  notification    │
 │ service   │  │  service    │  │   service        │
 │ :8081     │  │  :8082      │  │  :8083           │
 └─────┬─────┘  └──────┬──────┘  └────────▲─────────┘
       │ publishes      │ publishes        │ consumes
       └────────────────┴──── Kafka ───────┘
       user.created          order.placed
```

- **user-service** owns user identity. Publishes `user.created` on registration.
- **order-service** owns orders. Subscribes to `user.created` to warm caches and publishes `order.placed` when a new order arrives.
- **notification-service** is purely a consumer — listens to `order.placed`, records delivery, and exposes a recent-deliveries endpoint plus a custom Prometheus counter (`notifications_delivered_total`).
- **api-gateway** is the single entrypoint; routes `/api/users/**`, `/api/orders/**`, `/api/notifications/**` to the right service. CORS configured globally.

## Tech stack

- Java 17, Spring Boot 3.3, Spring Cloud Gateway 2023.0
- Apache Kafka (Confluent images)
- PostgreSQL 16 (with H2 fallback for local dev / tests)
- JPA / Hibernate
- Spring Security (stateless, JWT-ready)
- Micrometer + Prometheus + Grafana for metrics
- JUnit 5, Spring Kafka Test (`@EmbeddedKafka`), MockMvc
- Docker + Docker Compose

## Run locally (Docker Compose)

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000  (admin / admin) |
| User service direct | http://localhost:8081 |
| Order service direct | http://localhost:8082 |
| Notification service direct | http://localhost:8083 |

## Try it

```bash
# 1. create a user (publishes user.created event)
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com"}'

# 2. place an order for that user (publishes order.placed event)
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"item":"Spring in Action","amount":39.99}'

# 3. check notification-service picked it up
curl http://localhost:8080/api/notifications/recent
```

## Run tests

```bash
./mvnw test                   # or: mvn test
./mvnw -pl user-service test  # one module
```

Tests use `@EmbeddedKafka` so no external broker is needed. H2 backs JPA in tests.

## Project layout

```
microservices-demo/
├── pom.xml                    # parent BOM (Spring Boot 3.3, Spring Cloud 2023.0)
├── docker-compose.yml         # full local stack
├── user-service/              # :8081 — REST + JPA + Kafka producer
├── order-service/             # :8082 — REST + JPA + Kafka producer + consumer
├── notification-service/      # :8083 — Kafka consumer + REST view
├── api-gateway/               # :8080 — Spring Cloud Gateway
└── infra/
    ├── prometheus/prometheus.yml
    └── grafana/provisioning/
```

## Notes on design choices

- **Event-driven over synchronous calls** for inter-service communication. Decouples deployment, makes services independently scalable, survives downstream outages.
- **API gateway as single entrypoint** so client only knows one URL; routing, CORS, and rate-limiting live in one place.
- **Stateless security** — gateway propagates JWT to downstream services. Each service validates independently.
- **Actuator + Prometheus exposed per service** — no agent. Service metadata tags (`application`) let Grafana segment dashboards.

## Resume reference

This project demonstrates the following resume claim:

> *"Production-grade event-driven system (Spring Boot, Kafka, Docker, Kubernetes, GCP GKE) with CI/CD, Spring Cloud API Gateway, Prometheus/Grafana, Terraform IaC, OAuth 2.0/JWT, and GraphQL API layer."*

Current scope of this repo: Spring Boot, Kafka, Docker, Spring Cloud Gateway, Prometheus/Grafana, JWT-ready Spring Security. Kubernetes manifests, Terraform IaC, and GraphQL layer are roadmap items.

## Roadmap

- [ ] Kubernetes manifests (`k8s/` directory) for AKS/GKE deployment
- [ ] Terraform modules for GCP infra
- [ ] GraphQL layer over the REST APIs
- [ ] OAuth 2.0 with Spring Authorization Server
- [ ] Distributed tracing (OpenTelemetry → Jaeger)
- [ ] CI workflow (`.github/workflows/ci.yml`)

## License

MIT — see [LICENSE](LICENSE).
