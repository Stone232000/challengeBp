# Customer Service API - Microservicio de Gestión de Clientes

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.0--SNAPSHOT-blue.svg)](build.gradle)

Microservicio reactivo de gestión de clientes y personas construido con Spring Boot 3.5.7, WebFlux, R2DBC y Kafka. Implementa Hexagonal Architecture y sigue principios SOLID y patrones de diseño enterprise.

---

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Arquitectura](#-arquitectura)
- [Requisitos](#-requisitos)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Ejecución](#-ejecución)
- [API Endpoints](#-api-endpoints)
- [Documentación](#-documentación)
- [Testing](#-testing)
- [Deployment](#-deployment)

---

## ✨ Características

### Core Features
- ✅ **Reactive Programming** - 100% non-blocking con WebFlux y R2DBC
- ✅ **Hexagonal Architecture** - Ports & Adapters pattern
- ✅ **Event-Driven** - Publicación de eventos a Kafka
- ✅ **JWT Authentication** - Seguridad con tokens JWT
- ✅ **Multi-Environment** - Configuraciones para dev, staging, production
- ✅ **OpenAPI First** - Contract-first API design
- ✅ **Optimistic Locking** - Prevención de lost updates

### Security Features
- 🔐 JWT con custom claims (customerId, identification)
- 🔒 BCrypt password hashing (configurable strength)
- 🛡️ Conditional security (enable/disable per environment)
- ⚡ Reactive JWT filter (non-blocking validation)
- 🚫 Proper 401 error responses

### Database Features
- 🗄️ PostgreSQL con R2DBC (reactive)
- ⏱️ Auto-timestamp triggers
- 🔢 Auto-increment version for optimistic locking
- 🗑️ Soft delete (PATCH) vs Hard delete (DELETE)
- 📦 Persistable pattern for INSERT/UPDATE control

### Event-Driven Features
- 📨 Kafka producer reactivo
- 🏷️ Hierarchical topics: `banking.customer.events`
- 📋 Event headers: Correlation-ID, Event-Type, Timestamp
- 🔗 Correlation-ID propagation (HTTP → Kafka)
- ⚡ Fire-and-forget async events

---

## 🔧 Tecnologías

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 | Runtime |
| Spring Boot | 3.5.7 | Framework |
| Spring WebFlux | 6.x | Reactive Web |
| Spring Data R2DBC | 3.x | Reactive Database |
| PostgreSQL | 16 | Database |
| Apache Kafka | Latest | Event Streaming |
| Reactor Kafka | 1.3.23 | Reactive Kafka Producer |
| Spring Security | 6.x | Authentication & Authorization |
| JJWT | 0.12.6 | JWT Tokens |
| MapStruct | 1.6.3 | Object Mapping |
| Lombok | Latest | Boilerplate Reduction |
| SpringDoc OpenAPI | 2.8.14 | API Documentation |

---

## 🏗️ Arquitectura

### Hexagonal Architecture

```
┌─────────────────────────────────────────────────┐
│           PRIMARY ADAPTERS (Input)               │
│  REST Controllers, Global Exception Handler     │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│              APPLICATION LAYER                   │
│  Use Cases (Input Ports) + Service              │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│               DOMAIN LAYER                       │
│  Models (Person, Customer) + Exceptions         │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│        SECONDARY ADAPTERS (Output)               │
│  Persistence, Events, Security                   │
└─────────────────────────────────────────────────┘
```

### Capas

- **Domain**: Modelos de negocio (Person, Customer) y excepciones
- **Application**: Use Cases y Service (orquestación)
- **Infrastructure**: Adaptadores (REST, R2DBC, Kafka, Security)

---

## 📦 Requisitos

- **Java 21** o superior
- **Gradle 8.x** (incluido con wrapper)
- **Docker & Docker Compose** (para infraestructura)
- **PostgreSQL 16** (via Docker)
- **Apache Kafka** (via Docker)

---

## 🚀 Instalación

### 1. Clonar el repositorio
```bash
git clone https://github.com/Darwinpz/challenge.git
cd challenge/api-customer-service
```

### 2. Levantar infraestructura (PostgreSQL + Kafka)
```bash
cd ..
docker-compose up -d
```

### 3. Compilar el proyecto
```bash
./gradlew clean build
```

---

## ⚙️ Configuración

### Perfiles de Spring

| Perfil | Archivo | Uso |
|--------|---------|-----|
| `development` | `application.yaml` | Desarrollo local (security disabled) |
| `staging` | `application-staging.yaml` | Staging/Test (security enabled) |
| `production` | `application-production.yaml` | Producción (security required) |

### Variables de Entorno Principales

#### Development
```bash
SECURITY_ENABLED=false  # Deshabilitar JWT para desarrollo
LOG_LEVEL=DEBUG
```

#### Staging
```bash
SPRING_PROFILES_ACTIVE=staging
SECURITY_ENABLED=true
JWT_SECRET=staging-secret-key
DATABASE_URL=r2dbc:postgresql://staging-db:5432/customer_db
KAFKA_BOOTSTRAP_SERVERS=staging-kafka:9092
```

#### Production
```bash
SPRING_PROFILES_ACTIVE=production
JWT_SECRET=<strong-secret>  # REQUIRED
DATABASE_URL=<prod-db-url>  # REQUIRED
DATABASE_USERNAME=<user>    # REQUIRED
DATABASE_PASSWORD=<pass>    # REQUIRED
KAFKA_BOOTSTRAP_SERVERS=<kafka-url>  # REQUIRED
```

---

## ▶️ Ejecución

### Development (Sin JWT)
```bash
./gradlew bootRun
```

### Development (Con JWT)
```bash
export SECURITY_ENABLED=true
./gradlew bootRun
```

### Staging
```bash
export SPRING_PROFILES_ACTIVE=staging
./gradlew bootRun
```

### Production
```bash
export SPRING_PROFILES_ACTIVE=production
export JWT_SECRET=<secret>
# ... otras variables requeridas
java -jar build/libs/api-customer-service-1.0.0-SNAPSHOT.jar
```

### Con Docker
```bash
docker build -t api-customer-service .
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=staging \
  -e JWT_SECRET=my-secret \
  api-customer-service
```

---

## 📡 API Endpoints

### Base URL
```
http://localhost:8081/api/v1
```

### Endpoints Públicos (Sin JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/customers` | Crear customer (retorna JWT) |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/info` | Application info |
| GET | `/swagger-ui.html` | Swagger UI (dev/staging) |

### Endpoints Protegidos (Requieren JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/customers?page=0&size=20` | Listar customers (paginado) |
| GET | `/customers/{id}` | Obtener customer por ID |
| PUT | `/customers/{id}` | Actualizar customer completo |
| PATCH | `/customers/{id}` | Actualizar parcial / Soft delete |
| DELETE | `/customers/{id}` | Eliminar customer (hard delete) |
| GET | `/customers/{id}/validate` | Validar si customer está activo |

### Ejemplo: Crear Customer

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/customers \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: 12345" \
  -d '{
    "name": "Juan Pérez",
    "gender": "MALE",
    "age": 30,
    "identification": "1234567890",
    "address": "Calle 123",
    "phone": "0987654321",
    "password": "SecurePass123!",
    "state": true
  }'
```

**Response:**
```json
{
  "customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Juan Pérez",
  "gender": "MALE",
  "age": 30,
  "identification": "1234567890",
  "address": "Calle 123",
  "phone": "0987654321",
  "state": true,
  "createdAt": "2025-11-09T10:30:00Z",
  "updatedAt": "2025-11-09T10:30:00Z",
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### Ejemplo: Listar Customers (con JWT)

**Request:**
```bash
curl -X GET "http://localhost:8081/api/v1/customers?page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "X-Correlation-Id: 12345"
```

---

## 📚 Documentación

### OpenAPI / Swagger UI

- **Development/Staging**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/api-docs

### Documentación Adicional

- [IMPLEMENTATION-SUMMARY.md](IMPLEMENTATION-SUMMARY.md) - Resumen completo de implementación
- [HELP.md](HELP.md) - Spring Boot reference documentation

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run integration tests
./gradlew integrationTest
```

---

## 🚢 Deployment

### Kubernetes

```bash
# Development
kubectl apply -f helm/dev.yaml

# Staging/Test
kubectl apply -f helm/test.yaml

# Production
kubectl apply -f helm/prod.yaml
```

### Environment-Specific Configs

- `helm/dev.yaml` - Development Kubernetes config
- `helm/test.yaml` - Staging/Test Kubernetes config
- `helm/prod.yaml` - Production Kubernetes config

---

## 🎯 Principios y Patrones

### SOLID Principles
- ✅ Single Responsibility Principle
- ✅ Open/Closed Principle
- ✅ Liskov Substitution Principle
- ✅ Interface Segregation Principle
- ✅ Dependency Inversion Principle

### Design Patterns
- Hexagonal Architecture (Ports & Adapters)
- Repository Pattern
- Factory Method Pattern
- Builder Pattern
- Adapter Pattern
- Strategy Pattern
- Observer Pattern (Events)
- Result Pattern (AuthenticationResult)
- Value Object Pattern
- Persistable Pattern

---

## 📝 Eventos Kafka

### Tópico
`banking.customer.events`

### Event Types
- `CUSTOMER_CREATED`
- `CUSTOMER_UPDATED`
- `CUSTOMER_DELETED`

### Headers
- `correlation-id` - Distributed tracing
- `event-type` - Tipo de evento
- `timestamp` - Timestamp del evento

### Ejemplo de Evento
```json
{
  "eventType": "CUSTOMER_CREATED",
  "customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "identification": "1234567890",
  "name": "Juan Pérez",
  "timestamp": "2025-11-09T10:30:00Z"
}
```

---

## 👨‍💻 Autor

**Darwin Pilaloa Zea**
- Email: emiliopiedra2000@gmail.com
- Assessment Técnico - Senior Spring Boot Developer

---

## 📄 Licencia

Este proyecto es parte de un assessment técnico.

---

## 🤝 Contribución

Este es un proyecto de assessment técnico. No se aceptan contribuciones externas.

---

## 📞 Soporte

Para preguntas sobre este proyecto, contactar a: emiliopiedra2000@gmail.com
