# Order Processing Platform

A Spring Boot microservices backend where a customer places an order and the order is confirmed or
cancelled through a Kafka-driven choreography saga. Kong is the only entry point; services talk to
each other only through Kafka events.

> Work in progress: project structure and local infrastructure are set up; no business logic yet.
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

## Build and test

```bash
cd <service> && ./mvnw verify    # tests + JaCoCo report in target/site/jacoco
```
