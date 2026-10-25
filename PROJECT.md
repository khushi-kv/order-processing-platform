# PROJECT.md: Order Processing Platform

The working reference for this project: what is being built, the rules every service follows, and
where things stand. The source plan is *Order Processing Platform: Final Plan* (24 Oct 2026); this
file records where the build deliberately differs from it.

**Current state:** architecture only (service skeletons + local infrastructure). Sections 3 to 5
describe the target design; features are built day by day (see section 7).

## 1. Goal

A customer places an order through Kong. The order is **confirmed** only after Product reserves stock
and Payment succeeds; any failure **cancels** it and releases stock. Built in 10 days (26 Oct to
4 Nov 2026): 8 build days, 1 deployment day, 1 docs and interview-prep day.

## 2. Stack

| Area | Choice | Plan said |
|---|---|---|
| Language / framework | Java 25, Spring Boot 4.1.x | Java 21, Boot 3 |
| Database | PostgreSQL 18, one database per service | PostgreSQL 16 |
| Messaging | Apache Kafka 4.4, KRaft mode (no Zookeeper) | Kafka (KRaft) |
| Gateway | Kong 3.9, DB-less, `infra/kong/kong.yml` | Kong 3 |
| Auth | Spring Security + JWT (HS256) | same |
| Migrations | Flyway, `ddl-auto: validate` | Flyway |
| Quality | JUnit 5, Mockito, Testcontainers, JaCoCo, SonarQube (80% new-code gate) | same |
| Errors | To decide: inventory API format or RFC 9457 `ProblemDetail` | same format as inventory API |

## 3. Services

| Service | Port | DB | Publishes | Consumes |
|---|---|---|---|---|
| user | 8081 | `user_db` | none | none |
| product | 8082 | `product_db` | `stock-reserved`, `stock-failed` | `order-created`, `order-cancelled` |
| order | 8083 | `order_db` | `order-created`, `order-confirmed`, `order-cancelled` | `stock-reserved`, `stock-failed`, `payment-success`, `payment-failed` |
| payment | 8084 | `payment_db` | `payment-success`, `payment-failed` | `stock-reserved` |
| notification | 8085 | none | none | `order-confirmed`, `order-cancelled` |

Services never call each other over REST. Clients only call Kong on `:8000`.

## 4. Conventions (every service)

**Layout.** `controller → service → repository`, plus `dto`, `entity`, `event`, `kafka`,
`exception`, `config` under `com.orderplatform.<service>`. A service only has the packages it
needs (User has no Kafka; Notification has no database or HTTP API).

**Auth.**
- User Service signs the JWT with claims `iss=order-platform`, `sub=<userId>`, `email`, `role`
  (`CUSTOMER` | `ADMIN`).
- Kong checks the signature and issuer.
- Each service reads `userId`, `email` and `role` from the token and enforces roles itself with
  `@PreAuthorize` (Kong only checks that the token is valid).
- Registration always creates `CUSTOMER`. The first admin is seeded from `ADMIN_EMAIL` and
  `ADMIN_PASSWORD`.

**Errors.** One `@RestControllerAdvice` per service and the same JSON error format everywhere. Bean
Validation on requests. No stack traces in responses.

**Events.**
- Every event carries `eventId`, `orderId`, `correlationId`, `occurredAt` plus its payload.
- The Kafka key is `orderId`, so all events for one order stay in sequence.
- Topics have 3 partitions; each service has one consumer group named after the service.
- Every consumer is idempotent: it stores processed `eventId`s and skips repeats.
- Consumers retry with backoff, then send the message to `<topic>.DLT`.

**Database.** Each service owns its schema through Flyway (`db/migration/V<n>__*.sql`). There are no
shared tables and no cross-service joins.

**Config and secrets.** Secrets come from environment variables or a git-ignored `.env`, with a
`.env.example` beside it. Profiles are `dev` (infrastructure in Docker, services in the IDE) and
`prod` (everything in Docker).

**Observability.** Kong sets a correlation ID, which every service passes on in Kafka headers and
logs. Actuator health runs on every service.

## 5. Saga

- **Success:** Order saves `CREATED` → `order-created` → Product reserves stock → `stock-reserved`
  (with unit prices and total) → Payment charges → `payment-success` → Order marks `CONFIRMED` →
  `order-confirmed` → Notification.
- **Out of stock:** `stock-failed` → Order marks `CANCELLED` → Notification.
- **Payment fails:** `payment-failed` → Order marks `CANCELLED` → `order-cancelled` → Product
  releases the stock and Notification tells the customer.
- **Stuck order:** a scheduled job cancels orders still `CREATED` after 5 minutes.
- **Known limit:** saving an order and publishing its event are two separate steps. The outbox
  pattern is the first week-2 upgrade.

## 6. Local environment

| Container | Port | Notes |
|---|---|---|
| postgres | 5432 | `infra/postgres/init-databases.sql` creates the 4 databases |
| kafka | 9092 | KRaft, single node |
| kafka-ui | 8090 | |
| kong | 8000 (admin 8001, localhost only) | Upstreams use `host.docker.internal` while services run in the IDE |
| sonarqube | 9000 | Only with `--profile sonar` |

## 7. Progress

| Day | Scope | Status |
|---|---|---|
| 1 | Setup: infra, 5 skeletons, JaCoCo/Sonar, User Service with JWT | 🟡 infra + skeletons done; User Service and Sonar gate pending |
| 2 | Product: CRUD, stock, `@Version`, `stock_reservations` | ⬜ |
| 3 | Order: place/view/cancel, `order-created`; Product reserves | ⬜ |
| 4 | Payment + success path | ⬜ |
| 5 | Failure paths, stock release, stuck-order timeout | ⬜ |
| 6 | Idempotency, retries, DLTs; Notification | ⬜ |
| 7 | Kong: JWT, rate limit, CORS, correlation ID | ⬜ |
| 8 | Testcontainers saga tests, Swagger, Postman | ⬜ |
| 9 | Deployment: Dockerfiles, prod compose, VM, GitHub Actions | ⬜ |
| 10 | README, demo video, interview prep | ⬜ |

## 8. Out of scope (week 2)

Outbox pattern, refunds, shipping states, Redis cache, Prometheus + Grafana, Zipkin, real payment
gateway.
