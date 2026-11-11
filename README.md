# Order Processing Platform

A Spring Boot microservices backend where a customer places an order and the order is confirmed or
cancelled through a Kafka-driven choreography saga. Kong is the only entry point; services talk to
each other only through Kafka events.

> Work in progress: local infrastructure, the 5 service skeletons and the User Service are done.
> See [PROJECT.md](PROJECT.md) for the design, conventions and progress.

## Stack

Java 25 · Spring Boot 4.1 · Spring Security + JWT · Spring Data JPA · Flyway · Spring Kafka ·
PostgreSQL 18 · Apache Kafka 4.4 (KRaft) · Kong 3.9 (DB-less) · JUnit 5 · Mockito · JaCoCo ·
SonarQube · Docker Compose

## Structure

```
order-processing-platform/
├── docker-compose.yml        Postgres, Kafka (KRaft), Kafka UI, Kong, SonarQube
├── infra/
│   ├── kong/kong.yml         Gateway routes (DB-less)
│   └── postgres/             One database per service
├── user-service/             8081 · user_db
├── product-service/          8082 · product_db
├── order-service/            8083 · order_db
├── payment-service/          8084 · payment_db
└── notification-service/     8085 · no database
```

Each service is its own Maven project with the same package layout under
`com.orderplatform.<service>`: `controller`, `service`, `repository`, `dto`, `entity`, `event`,
`kafka`, `exception`, `config` (only the packages the service needs), and Flyway migrations in
`src/main/resources/db/migration`.

## Run locally

Prerequisites: JDK 25, Docker Desktop.

```bash
# Infrastructure: Postgres, Kafka, Kafka UI, Kong
docker compose up -d

# A service (from its folder)
cd user-service && ./mvnw spring-boot:run
```

| URL | What |
|---|---|
| http://localhost:8000 | Kong gateway (all API calls go here) |
| http://localhost:8090 | Kafka UI |
| http://localhost:9000 | SonarQube (`docker compose --profile sonar up -d sonarqube`) |

Each service reads its secrets from a git-ignored `.env` in its own folder; copy `.env.example`
and fill in real values.

## User Service (8081)

Registration, login and profiles. Login returns an HS256 JWT (1 hour) with the claims `iss`, `sub`
(user id), `email` and `role`. Registration always creates a `CUSTOMER`; the first `ADMIN` is
seeded at startup from `ADMIN_EMAIL` and `ADMIN_PASSWORD`.

| Method | Path | Auth | What |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create a customer account (201) |
| POST | `/api/auth/login` | none | Returns `accessToken`, `tokenType`, `expiresIn` |
| GET | `/api/users/me` | Bearer | Current user's profile |
| PUT | `/api/users/me` | Bearer | Update the current user's name |

Swagger UI: http://localhost:8081/swagger-ui.html. Log in, then paste the `accessToken` into
**Authorize** to call the protected endpoints.

Required in `user-service/.env`: `JWT_SECRET` (at least 32 characters; the service will not start
without it), plus `ADMIN_EMAIL` and `ADMIN_PASSWORD` to seed the admin.

## Build and test

```bash
cd <service> && ./mvnw verify    # tests + JaCoCo report in target/site/jacoco
```

SonarQube analysis (quality gate `order-platform`: 80% coverage on new code, no new issues). Put a
global analysis token in the root `.env` as `SONAR_TOKEN`, then from a service folder:

```bash
./mvnw clean verify sonar:sonar -Dsonar.token=$SONAR_TOKEN
```
