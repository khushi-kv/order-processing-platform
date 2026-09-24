# Event-Driven Order Processing Platform

A production-grade **Spring Boot Microservices Platform** built with **Java 17**, featuring **Apache Kafka** event streaming, **Redis** caching, **Spring Cloud Gateway**, and **Resilience4j** circuit breakers.

## 🏗️ Architecture (Current Progress)

This repository is a **mono-repo** that currently contains the foundational infrastructure and the first core microservice.

### 1. Infrastructure
* **PostgreSQL**: Relational storage (running on port `5432`).
* **Redis**: Distributed caching (running on port `6379`).
* **Kafka & Zookeeper**: Message broker for asynchronous event-driven communication (running on port `9092`).

### 2. Auth Service (`auth-service`)
A fully stateless, highly secure microservice responsible for user identity and token generation.
* **Tech**: Spring Boot, Spring Security, Spring Data JPA, BCrypt, JJWT.
* **Security**: Enforces strict password complexity rules on registration, securely verifies credentials against BCrypt hashes, and issues cryptographically signed JWT tokens for downstream authorization.
* **Secrets Management**: Implements `.env` variable injection to securely manage the JWT Secret Key out of source control.

---

## 🚀 Getting Started

### 1. Start the Infrastructure
Make sure you have Docker installed, then run the background services:
```bash
docker-compose up -d
```

### 2. Configure the Auth Service Environment
Create a `.env` file at the root of the `auth-service` folder (`auth-service/.env`) and add a secure secret key:
```env
JWT_SECRET=your_super_secret_64_character_hex_string_here
```

### 3. Run the Auth Service
Navigate to the `auth-service` directory and boot the application (runs on port `8081`):
```bash
cd auth-service
./mvnw spring-boot:run
```

---

## 📖 API Documentation

### Auth Service (`localhost:8081`)

#### 1. Register a new user
```http
POST /auth/register
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "StrongPassword123"
}
```

#### 2. Login to get JWT Token
```http
POST /auth/login
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "StrongPassword123"
}
```

---
*Stay tuned: Order Service and Inventory Service are currently in development!*
