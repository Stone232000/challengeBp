# Customer Service - Implementation Summary

## 📋 Resumen Ejecutivo

Desarrollo de microservicio **api-customer-service** siguiendo las mejores prácticas de desarrollo Senior para Spring Boot, cumpliendo con los requisitos del assessment técnico.

---

## 🏗️ Arquitectura Implementada

### Hexagonal Architecture (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API (Primary Adapter)                │
│  CustomerController, HealthController, GlobalExceptionHandler│
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                   Application Layer                          │
│  Input Ports (Use Cases Interfaces)                          │
│  - CreateCustomerUseCase                                     │
│  - GetCustomerUseCase                                        │
│  - UpdateCustomerUseCase                                     │
│  - DeleteCustomerUseCase                                     │
│                                                               │
│  Service Implementation (CustomerService)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    Domain Layer                              │
│  - Person (Domain Model)                                     │
│  - Customer (Aggregate Root)                                 │
│  - Domain Exceptions                                         │
│  - Business Logic Methods                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│              Infrastructure Layer (Secondary Adapters)        │
│                                                               │
│  Persistence (R2DBC):                                        │
│  - CustomerPersistenceAdapter                                │
│  - PersonR2dbcRepository                                     │
│  - CustomerR2dbcRepository                                   │
│                                                               │
│  Events (Kafka):                                             │
│  - KafkaEventPublisherAdapter                                │
│  - ReactiveKafkaProducerTemplate                             │
│                                                               │
│  Security:                                                    │
│  - BCryptPasswordEncoderAdapter                              │
│  - JwtTokenAdapter                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Características Implementadas

### 1. **Domain Layer (Capa de Dominio)**
- ✅ `Person` - Domain model sin validaciones (clean POJO)
- ✅ `Customer` - Aggregate Root con lógica de negocio
- ✅ Excepciones personalizadas del dominio:
  - `CustomerNotFoundException`
  - `CustomerAlreadyExistsException`
  - `InvalidPasswordException`
  - `CustomerInactiveException`
  - `OptimisticLockException`

### 2. **Application Layer (Capa de Aplicación)**
- ✅ **Input Ports (Interfaces de Use Cases)**:
  - `CreateCustomerUseCase`
  - `GetCustomerUseCase`
  - `UpdateCustomerUseCase`
  - `DeleteCustomerUseCase`

- ✅ **Output Ports (Interfaces de Repositorio y Servicios)**:
  - `CustomerRepositoryPort`
  - `EventPublisherPort`
  - `PasswordEncoderPort`
  - `JwtTokenPort`

- ✅ **Service Implementation**:
  - `CustomerService` - Orquesta todos los use cases
  - Publicación de eventos a Kafka (customer.created, customer.updated, customer.deleted)

### 3. **Infrastructure Layer (Capa de Infraestructura)**

#### Persistence (R2DBC Reactive)
- ✅ `PersonEntity` - Entidad R2DBC para tabla `person` (implements Persistable)
- ✅ `CustomerEntity` - Entidad R2DBC para tabla `customer` (implements Persistable)
- ✅ `PersonR2dbcRepository` - Repositorio reactivo
- ✅ `CustomerR2dbcRepository` - Repositorio reactivo
- ✅ `CustomerPersistenceAdapter` - Implementa `CustomerRepositoryPort`
- ✅ `CustomerPersistenceMapper` - MapStruct para Entity ↔ Domain
- ✅ **Optimistic Locking** - Version field con auto-incremento via triggers
- ✅ **Persistable Pattern** - Control manual de INSERT vs UPDATE
- ✅ **Hard Delete** - DELETE endpoint elimina registros de BD
- ✅ **Soft Delete** - PATCH endpoint desactiva customer (state=false)

#### Event Publishing (Kafka)
- ✅ `KafkaEventPublisherAdapter` - Implementa `EventPublisherPort`
- ✅ `KafkaProducerConfig` - Configuración de Reactor Kafka
- ✅ Publicación a tópico jerárquico: `banking.customer.events`
- ✅ **Kafka Headers** - Correlation-ID y metadata en headers
- ✅ **Correlation-ID Propagation** - Propagación desde controller hasta Kafka
- ✅ **Event Types**: CUSTOMER_CREATED, CUSTOMER_UPDATED, CUSTOMER_DELETED

#### Security
- ✅ `BCryptPasswordEncoderAdapter` - Implementa `PasswordEncoderPort`
- ✅ `JwtTokenAdapter` - Implementa `JwtTokenPort` (generación y validación JWT)
- ✅ `SecurityConfig` - Configuración Spring Security (BCrypt, CORS, JWT)
- ✅ `JwtAuthenticationWebFilter` - Filtro reactivo para validación JWT
- ✅ `JwtAuthenticationEntryPoint` - Manejo de errores 401 UNAUTHORIZED
- ✅ `AuthenticationResult` - Value Object para resultados de autenticación
- ✅ **Seguridad Condicional** - Habilitación/deshabilitación via `app.security.enabled`

#### REST Controllers
- ✅ `CustomerController` - Implementa `CustomersApi` (generado por OpenAPI)
  - GET /customers (paginado)
  - POST /customers (crear con JWT)
  - GET /customers/{id}
  - PUT /customers/{id}
  - PATCH /customers/{id}
  - DELETE /customers/{id}
  - GET /customers/{id}/validate
- ✅ `HealthController` - Health check endpoint
- ✅ `GlobalExceptionHandler` - Manejo global de excepciones con `ErrorResponse` del contrato

#### Mappers
- ✅ `CustomerRestMapper` - MapStruct para DTO ↔ Domain
- ✅ `CustomerPersistenceMapper` - MapStruct para Entity ↔ Domain

---

## 🎯 Principios SOLID Aplicados

### 1. **Single Responsibility Principle (SRP)**
- ✅ Cada clase tiene una única responsabilidad
- ✅ Separación clara entre Domain, Application e Infrastructure
- ✅ Servicios especializados (CustomerService, PasswordEncoder, JwtToken)

### 2. **Open/Closed Principle (OCP)**
- ✅ Domain models extensibles sin modificación
- ✅ Jerarquía de excepciones (DomainException base)
- ✅ GlobalExceptionHandler fácil de extender

### 3. **Liskov Substitution Principle (LSP)**
- ✅ Composición sobre herencia (Customer HAS-A Person)
- ✅ Todas las implementaciones de ports son intercambiables

### 4. **Interface Segregation Principle (ISP)**
- ✅ Interfaces pequeñas y específicas (CreateUseCase, GetUseCase, etc.)
- ✅ Ports segregados por responsabilidad

### 5. **Dependency Inversion Principle (DIP)**
- ✅ Capas superiores dependen de abstracciones (ports)
- ✅ Inyección de dependencias vía constructor
- ✅ Domain no depende de Infrastructure

---

## 🎨 Patrones de Diseño Implementados

### Arquitectónicos
- ✅ **Hexagonal Architecture (Ports & Adapters)**
- ✅ **Event-Driven Architecture**
- ✅ **Layered Architecture**

### Creacionales
- ✅ **Builder Pattern** - Lombok @Builder en domain models
- ✅ **Factory Method** - Customer.create(), Person.create()

### Estructurales
- ✅ **Adapter Pattern** - Todos los adaptadores de infraestructura
- ✅ **Facade Pattern** - CustomerService como fachada de use cases
- ✅ **Repository Pattern** - Abstracción de persistencia
- ✅ **Mapper Pattern** - MapStruct para conversiones

### Comportamentales
- ✅ **Strategy Pattern** - PasswordEncoderPort, JwtTokenPort
- ✅ **Observer Pattern** - Event publishing a Kafka
- ✅ **Command Pattern** - Use cases como comandos

### Otros
- ✅ **DTO Pattern** - Separación entre DTOs (OpenAPI) y Domain Models
- ✅ **Optimistic Locking Pattern** - Version field en Customer con auto-increment
- ✅ **Filter Pattern** - Correlation ID y Request ID filters
- ✅ **Result Pattern** - AuthenticationResult para encapsular success/failure
- ✅ **Value Object Pattern** - AuthenticationResult immutable con Builder
- ✅ **Persistable Pattern** - Control manual INSERT vs UPDATE en R2DBC

---

## 🔧 Tecnologías y Stack

### Core
- ☕ **Java 21**
- 🍃 **Spring Boot 3.5.7**
- ⚛️ **Spring WebFlux** (Reactive)
- 🗄️ **Spring Data R2DBC** (Reactive Database)
- 🐘 **PostgreSQL 16** (with Triggers & Optimistic Locking)

### Security
- 🔐 **Spring Security** (Reactive WebFlux)
- 🔑 **JWT (JJWT 0.12.6)** (Custom claims: customerId, identification)
- 🔒 **BCrypt** (Configurable strength per environment)
- 🛡️ **Conditional Security** (Enable/disable via property)

### Messaging
- 📨 **Apache Kafka** (Hierarchical topics: banking.customer.events)
- ⚛️ **Reactor Kafka 1.3.23** (Reactive producer with headers)
- 🔗 **Correlation-ID Propagation** (HTTP → Kafka)

### API & Documentation
- 📜 **OpenAPI 3.0.3** (Contract-First)
- 🔄 **OpenAPI Generator** (build-time code generation)
- 📚 **SpringDoc OpenAPI** (Swagger UI - environment configurable)

### Mapping & Utilities
- 🗺️ **MapStruct 1.6.3**
- 📦 **Lombok**

### Logging & Monitoring
- 📊 **Logback** (Structured JSON logging)
- 📈 **Logstash Encoder 8.0**
- 🏥 **Spring Actuator**
- 📏 **Micrometer Prometheus**

---

## 📁 Estructura de Archivos Creados

```
api-customer-service/
├── src/main/java/com/dpilaloa/api/customer/service/
│   ├── ApiCustomerServiceApplication.java ✅
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Person.java ✅
│   │   │   └── Customer.java ✅
│   │   └── exception/
│   │       ├── DomainException.java ✅
│   │       ├── CustomerNotFoundException.java ✅
│   │       ├── CustomerAlreadyExistsException.java ✅
│   │       ├── InvalidPasswordException.java ✅
│   │       ├── CustomerInactiveException.java ✅
│   │       └── OptimisticLockException.java ✅
│   │
│   ├── application/
│   │   ├── ports/
│   │   │   ├── input/
│   │   │   │   ├── CreateCustomerUseCase.java ✅
│   │   │   │   ├── GetCustomerUseCase.java ✅
│   │   │   │   ├── UpdateCustomerUseCase.java ✅
│   │   │   │   └── DeleteCustomerUseCase.java ✅
│   │   │   └── output/
│   │   │       ├── CustomerRepositoryPort.java ✅
│   │   │       ├── EventPublisherPort.java ✅
│   │   │       ├── PasswordEncoderPort.java ✅
│   │   │       └── JwtTokenPort.java ✅
│   │   └── service/
│   │       └── CustomerService.java ✅
│   │
│   └── infraestructure/
│       └── adapter/
│           ├── input/rest/
│           │   ├── CustomerController.java ✅
│           │   ├── HealthController.java ✅
│           │   ├── exception/
│           │   │   └── GlobalExceptionHandler.java ✅
│           │   └── mapper/
│           │       └── CustomerRestMapper.java ✅
│           │
│           ├── output/
│           │   ├── persistence/
│           │   │   ├── entity/
│           │   │   │   ├── PersonEntity.java ✅
│           │   │   │   └── CustomerEntity.java ✅
│           │   │   ├── repository/
│           │   │   │   ├── PersonR2dbcRepository.java ✅
│           │   │   │   └── CustomerR2dbcRepository.java ✅
│           │   │   ├── mapper/
│           │   │   │   └── CustomerPersistenceMapper.java ✅
│           │   │   └── CustomerPersistenceAdapter.java ✅
│           │   │
│           │   ├── event/
│           │   │   └── KafkaEventPublisherAdapter.java ✅
│           │   │
│           │   └── security/
│           │       ├── BCryptPasswordEncoderAdapter.java ✅
│           │       └── JwtTokenAdapter.java ✅
│           │
│           └── config/
│               ├── SecurityConfig.java ✅
│               ├── KafkaProducerConfig.java ✅
│               └── WebFluxConfig.java ✅
│
└── src/main/resources/
    ├── application.yaml ✅ (Development - Security disabled by default)
    ├── application-staging.yaml ✅ (Staging - Security enabled, Swagger enabled)
    ├── application-production.yaml ✅ (Production - Security required, Swagger disabled)
    ├── openapi.yaml ✅
    └── logback-spring.xml ✅

└── helm/
    ├── dev.yaml ✅ (Kubernetes config for development)
    ├── test.yaml ✅ (Kubernetes config for staging/test)
    └── prod.yaml ✅ (Kubernetes config for production)

└── database/
    └── init-customer-db.sql ✅ (PostgreSQL schema with triggers)
```

---

## 🚀 Características Destacadas

### Clean Code
- ✅ Sin código redundante
- ✅ Nombres descriptivos
- ✅ Métodos pequeños y específicos
- ✅ Comentarios explicando patrones y principios SOLID

### Reactive Programming
- ✅ Totalmente no-bloqueante (Mono/Flux)
- ✅ R2DBC para base de datos reactiva
- ✅ Reactor Kafka para eventos reactivos
- ✅ WebFlux para HTTP reactivo

### Logging Estructurado
- ✅ JSON format para producción
- ✅ Console colorizado para desarrollo
- ✅ Correlation ID y Request ID tracking
- ✅ MDC (Mapped Diagnostic Context)
- ✅ Filtros WebFlux para tracing distribuido

### Security
- ✅ BCrypt con strength configurable por ambiente
- ✅ JWT con claims customizados (customerId, identification)
- ✅ CORS configurado por ambiente
- ✅ Public endpoints configurables
- ✅ **Seguridad Condicional** - Habilitación/deshabilitación via property
- ✅ **JWT Filter Reactivo** - Validación no-bloqueante de tokens
- ✅ **Error Handling** - Respuestas 401 consistentes con JSON
- ✅ **Result Pattern** - AuthenticationResult para encapsular validaciones

### Event-Driven
- ✅ Tópico jerárquico: `banking.customer.events`
- ✅ Fire-and-forget events (non-blocking)
- ✅ Idempotencia habilitada
- ✅ Compresión Snappy
- ✅ **Kafka Headers** - Correlation-ID, Event-Type, Timestamp
- ✅ **Event Propagation** - Correlation-ID desde HTTP hasta Kafka
- ✅ **Event Types** - CREATED, UPDATED, DELETED

### Multi-Environment Configuration
- ✅ **Development** (application.yaml)
  - Security: Disabled by default (`SECURITY_ENABLED=false`)
  - Logging: DEBUG level
  - Swagger: Enabled
  - JWT Expiration: 24 hours
  - Password Strength: 10

- ✅ **Staging** (application-staging.yaml)
  - Security: Enabled
  - Logging: INFO level
  - Swagger: Enabled (for testing)
  - JWT Expiration: 12 hours (shorter for testing)
  - Password Strength: 10
  - Kafka Topic: `banking.customer.events.staging`

- ✅ **Production** (application-production.yaml)
  - Security: Required (no defaults, must use env vars)
  - Logging: WARN/INFO level
  - Swagger: Disabled (security)
  - JWT Expiration: 24 hours
  - Password Strength: 12 (stronger)
  - Error Details: Hidden (no stack traces, binding errors)
  - Actuator Health: Details never shown
  - Kafka Topic: `banking.customer.events`

### Database Features
- ✅ **PostgreSQL Triggers**
  - `update_person_updated_at()` - Auto-update timestamp on person table
  - `update_customer_updated_at()` - Auto-update timestamp + version increment on customer table

- ✅ **Optimistic Locking**
  - Version field auto-incremented via trigger
  - Prevents lost updates in concurrent scenarios

- ✅ **Soft Delete vs Hard Delete**
  - PATCH endpoint: Soft delete (sets state=false)
  - DELETE endpoint: Hard delete (removes from database)

---

## 🚀 Cómo Ejecutar

### Development (Sin JWT)
```bash
# Opción 1: Directamente (security disabled por defecto)
./gradlew bootRun

# Opción 2: Explícitamente sin seguridad
export SECURITY_ENABLED=false
./gradlew bootRun

# Accede sin necesidad de JWT
curl http://localhost:8081/api/v1/customers
```

### Development (Con JWT para testing)
```bash
# Habilitar seguridad en desarrollo
export SECURITY_ENABLED=true
./gradlew bootRun

# Necesitas JWT para acceder
curl -H "Authorization: Bearer <token>" http://localhost:8081/api/v1/customers
```

### Staging
```bash
# Activar perfil staging
export SPRING_PROFILES_ACTIVE=staging
./gradlew bootRun

# O con JAR
java -jar build/libs/api-customer-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=staging
```

### Production
```bash
# Activar perfil production (REQUIERE variables de entorno)
export SPRING_PROFILES_ACTIVE=production
export JWT_SECRET=<strong-secret-key>
export DATABASE_URL=r2dbc:postgresql://prod-db:5432/customer_db
export DATABASE_USERNAME=customer_user
export DATABASE_PASSWORD=<secure-password>
export KAFKA_BOOTSTRAP_SERVERS=prod-kafka:9092

java -jar build/libs/api-customer-service-1.0.0-SNAPSHOT.jar
```

### Kubernetes
```bash
# Development
kubectl apply -f helm/dev.yaml

# Staging/Test
kubectl apply -f helm/test.yaml

# Production
kubectl apply -f helm/prod.yaml
```

### Endpoints Disponibles

#### Sin Autenticación (Public)
- `POST /api/v1/customers` - Crear customer (retorna JWT)
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Application info
- `GET /swagger-ui.html` - Swagger UI (dev/staging only)

#### Con Autenticación JWT (Protected)
- `GET /api/v1/customers` - Listar customers (paginado)
- `GET /api/v1/customers/{id}` - Obtener customer por ID
- `PUT /api/v1/customers/{id}` - Actualizar customer completo
- `PATCH /api/v1/customers/{id}` - Actualizar parcial (soft delete con state=false)
- `DELETE /api/v1/customers/{id}` - Eliminar customer (hard delete)
- `GET /api/v1/customers/{id}/validate` - Validar customer activo

---

## 📝 Próximos Pasos (Opcionales)

1. **Testing**
   - Unit tests con JUnit 5 y Mockito
   - Integration tests con Testcontainers
   - BlockHound para detectar blocking calls

2. **Monitoring**
   - Grafana dashboards
   - Prometheus metrics
   - Distributed tracing con Zipkin

3. **CI/CD**
   - GitHub Actions pipeline
   - Docker image building
   - Kubernetes deployment

---

## 👨‍💻 Autor

**Darwin Pilaloa Zea**
Email: dpilaloazea@gmail.com
Assessment Técnico - Senior Spring Boot Developer
Versión: 1.0.0-SNAPSHOT
Fecha: Noviembre 2025

---

## 📚 Referencias

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Reactor Kafka](https://projectreactor.io/docs/kafka/release/reference/)
- [OpenAPI Specification](https://swagger.io/specification/)
