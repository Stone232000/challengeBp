# API Account Service

Microservicio para la gestión de cuentas bancarias y movimientos financieros, desarrollado con Spring Boot 3.x y arquitectura hexagonal.

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [API Documentation](#api-documentation)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [Testing](#testing)
- [Desarrollo](#desarrollo)

## Descripción General

El **API Account Service** es un microservicio reactivo que gestiona:

- **Cuentas bancarias**: Creación, consulta, actualización y eliminación de cuentas
- **Movimientos financieros**: Registro de depósitos, retiros y reversas con control de saldo
- **Reportes**: Generación de estados de cuenta y resúmenes de movimientos

### Características principales

- ✅ **OpenAPI Contract-First**: API definida mediante especificación OpenAPI 3.0
- ✅ **Arquitectura Hexagonal**: Separación clara entre dominio, aplicación e infraestructura
- ✅ **Programación Reactiva**: Spring WebFlux con Reactor (Mono/Flux)
- ✅ **Persistencia R2DBC**: Acceso reactivo a PostgreSQL
- ✅ **MapStruct**: Mapeo compile-time entre capas
- ✅ **Kafka**: Comunicación asíncrona entre microservicios
- ✅ **Optimistic Locking**: Control de concurrencia con versionado
- ✅ **Cache de clientes**: Sincronización desde api-customer-service

## Arquitectura

### Arquitectura Hexagonal (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              INPUT ADAPTERS (Driving)                │   │
│  │  • REST Controllers (OpenAPI)                        │   │
│  │  • Kafka Consumers                                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  APPLICATION LAYER                   │   │
│  │  • Use Cases (Ports IN)                              │   │
│  │  • Service Implementations                           │   │
│  │  • DTOs & Mappers                                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    DOMAIN LAYER                      │   │
│  │  • Entities (Account, Movement, Customer)            │   │
│  │  • Value Objects                                     │   │
│  │  • Domain Services                                   │   │
│  │  • Business Rules & Validations                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              OUTPUT ADAPTERS (Driven)                │   │
│  │  • R2DBC Repositories (PostgreSQL)                   │   │
│  │  • WebClient (HTTP Clients)                          │   │
│  │  • Kafka Producers                                   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Flujo de Datos

1. **Request** → REST Controller (OpenAPI interface)
2. **Mapping** → OpenAPI DTO → Domain Model (MapStruct)
3. **Use Case** → Business logic execution
4. **Persistence** → R2DBC reactive repositories
5. **Response** → Domain Model → OpenAPI DTO (MapStruct)

## Tecnologías

### Core

- **Java 17** - LTS version
- **Spring Boot 3.4.1** - Framework base
- **Spring WebFlux** - Programación reactiva
- **Project Reactor** - Reactive Streams implementation
- **Gradle 8.14.3** - Build tool

### Persistence

- **Spring Data R2DBC** - Reactive database access
- **PostgreSQL** - Base de datos relacional
- **R2DBC PostgreSQL Driver** - Driver reactivo

### API & Mapping

- **OpenAPI Generator 7.13.0** - Generación de código desde OpenAPI spec
- **MapStruct 1.6.3** - Compile-time bean mapping
- **Springdoc OpenAPI** - Documentación interactiva (Swagger UI)

### Messaging

- **Spring Kafka** - Integración con Apache Kafka
- **Kafka Reactor** - Soporte reactivo para Kafka

### Testing

- **JUnit 5** - Framework de testing
- **Reactor Test** - Testing de componentes reactivos
- **Testcontainers** - Contenedores para integration tests

### Development Tools

- **Lombok** - Reducción de boilerplate
- **Spring DevTools** - Hot reload en desarrollo

## Estructura del Proyecto

```
api-account-service/
├── src/
│   ├── main/
│   │   ├── java/com/espiedra/api/account/service/
│   │   │   ├── domain/                          # Capa de dominio
│   │   │   │   ├── model/                       # Entidades de dominio
│   │   │   │   │   ├── Account.java            # Cuenta bancaria
│   │   │   │   │   ├── Movement.java           # Movimiento financiero
│   │   │   │   │   └── Customer.java           # Cliente
│   │   │   │   ├── exception/                   # Excepciones de negocio
│   │   │   │   └── service/                     # Servicios de dominio
│   │   │   │
│   │   │   ├── application/                     # Capa de aplicación
│   │   │   │   ├── port/
│   │   │   │   │   ├── input/                   # Puertos de entrada (Use Cases)
│   │   │   │   │   │   ├── CreateAccountUseCase.java
│   │   │   │   │   │   ├── GetAccountUseCase.java
│   │   │   │   │   │   ├── CreateMovementUseCase.java
│   │   │   │   │   │   └── GenerateAccountStatementUseCase.java
│   │   │   │   │   └── output/                  # Puertos de salida (SPI)
│   │   │   │   │       ├── AccountRepository.java
│   │   │   │   │       ├── MovementRepository.java
│   │   │   │   │       └── CustomerServiceClient.java
│   │   │   │   ├── service/                     # Implementación de Use Cases
│   │   │   │   └── dto/                         # DTOs de aplicación
│   │   │   │
│   │   │   └── infrastructure/                  # Capa de infraestructura
│   │   │       ├── adapter/
│   │   │       │   ├── input/
│   │   │       │   │   └── rest/               # Adaptadores REST
│   │   │       │   │       ├── AccountController.java      # Implementa AccountsApi
│   │   │       │   │       ├── MovementController.java     # Implementa MovementsApi
│   │   │       │   │       ├── ReportController.java       # Implementa ReportsApi
│   │   │       │   │       └── mapper/                     # Mappers OpenAPI ↔ Domain
│   │   │       │   │           ├── AccountApiMapper.java   # MapStruct mapper
│   │   │       │   │           ├── MovementApiMapper.java  # MapStruct mapper
│   │   │       │   │           └── ReportApiMapper.java    # MapStruct mapper
│   │   │       │   │
│   │   │       │   └── output/
│   │   │       │       ├── persistence/         # Adaptador de persistencia
│   │   │       │       │   ├── entity/          # Entidades JPA/R2DBC
│   │   │       │       │   │   ├── AccountEntity.java
│   │   │       │       │   │   ├── MovementEntity.java
│   │   │       │       │   │   └── CustomerEntity.java
│   │   │       │       │   ├── repository/      # Repositorios R2DBC
│   │   │       │       │   │   ├── R2dbcAccountRepository.java
│   │   │       │       │   │   ├── R2dbcMovementRepository.java
│   │   │       │       │   │   └── R2dbcCustomerRepository.java
│   │   │       │       │   ├── mapper/          # Mappers Domain ↔ Entity
│   │   │       │       │   │   ├── AccountEntityMapper.java
│   │   │       │       │   │   ├── MovementEntityMapper.java
│   │   │       │       │   │   └── CustomerEntityMapper.java
│   │   │       │       │   └── AccountPersistenceAdapter.java
│   │   │       │       │
│   │   │       │       └── messaging/           # Adaptador Kafka
│   │   │       │           └── kafka/
│   │   │       │               ├── consumer/
│   │   │       │               │   └── CustomerEventConsumer.java
│   │   │       │               └── config/
│   │   │       │
│   │   │       └── config/                      # Configuración Spring
│   │   │           ├── OpenApiConfig.java
│   │   │           ├── R2dbcConfig.java
│   │   │           └── KafkaConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                  # Configuración principal
│   │       ├── application-dev.yml              # Profile desarrollo
│   │       ├── application-prod.yml             # Profile producción
│   │       ├── db/
│   │       │   └── migration/                   # Scripts SQL (Flyway/Liquibase)
│   │       └── openapi/
│   │           └── account-service-api.yaml     # Especificación OpenAPI
│   │
│   └── test/                                    # Tests
│
├── build/
│   └── generated/                               # Código generado por OpenAPI
│       └── src/main/java/.../api/              # Interfaces y modelos OpenAPI
│
├── build.gradle                                 # Configuración Gradle
├── settings.gradle
└── README.md                                    # Este archivo
```

## API Documentation

### OpenAPI Specification

La API está completamente definida en el archivo `src/main/resources/openapi/account-service-api.yaml`.

### Endpoints Principales

#### Accounts API

```
POST   /accounts                    - Crear nueva cuenta
GET    /accounts                    - Listar cuentas (con filtros)
GET    /accounts/{accountNumber}    - Obtener cuenta por número
PUT    /accounts/{accountNumber}    - Actualizar cuenta
DELETE /accounts/{accountNumber}    - Eliminar cuenta
PATCH  /accounts/{accountNumber}    - Actualización parcial
```

#### Movements API

```
POST   /movements                   - Crear movimiento (depósito/retiro)
GET    /movements                   - Listar movimientos (con filtros)
GET    /movements/{movementId}      - Obtener movimiento por ID
POST   /movements/{movementId}/reverse - Reversar movimiento
```

#### Reports API

```
GET    /reports/account-statement/{customerId}  - Estado de cuenta
GET    /reports/movements-summary               - Resumen de movimientos
```

### Swagger UI

Una vez iniciada la aplicación, la documentación interactiva está disponible en:

```
http://localhost:8082/swagger-ui.html
```

### OpenAPI JSON

```
http://localhost:8082/v3/api-docs
```

## Configuración

### Variables de Entorno

#### Database (R2DBC)

```properties
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/account_db
spring.r2dbc.username=account_user
spring.r2dbc.password=account_password
```

#### Kafka

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=account-service-group
```

#### Application

```properties
server.port=8082
spring.application.name=api-account-service
```

### Perfiles de Configuración

- **dev**: Desarrollo local con logs detallados
- **test**: Configuración para tests con base de datos en memoria
- **prod**: Configuración de producción optimizada

Activar perfil:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Ejecución

### Prerequisitos

- **JDK 17+** instalado
- **Docker** (para PostgreSQL y Kafka)
- **Gradle 8.x** (incluido vía wrapper)

### 1. Iniciar infraestructura con Docker Compose

```bash
cd ../infrastructure  # Directorio raíz del proyecto
docker-compose up -d postgres kafka zookeeper
```

Esto iniciará:
- PostgreSQL en `localhost:5432`
- Kafka en `localhost:9092`
- Zookeeper en `localhost:2181`

### 2. Compilar el proyecto

```bash
./gradlew clean build
```

Este comando:
- Limpia builds anteriores
- Genera código desde OpenAPI spec
- Genera implementaciones de MapStruct
- Compila el código Java
- Ejecuta los tests
- Genera el JAR ejecutable

### 3. Ejecutar la aplicación

#### Opción A: Con Gradle

```bash
./gradlew bootRun
```

#### Opción B: JAR ejecutable

```bash
java -jar build/libs/api-account-service-1.0.0.jar
```

#### Opción C: Docker

```bash
docker build -t api-account-service .
docker run -p 8082:8082 api-account-service
```

### 4. Verificar ejecución

```bash
curl http://localhost:8082/actuator/health
```

Respuesta esperada:
```json
{
  "status": "UP"
}
```

## Testing

### Ejecutar todos los tests

```bash
./gradlew test
```

### Ejecutar tests específicos

```bash
./gradlew test --tests AccountServiceTest
```

### Reporte de tests

Los reportes HTML se generan en:
```
build/reports/tests/test/index.html
```

### Coverage

```bash
./gradlew jacocoTestReport
```

Reporte en: `build/reports/jacoco/test/html/index.html`

## Desarrollo

### Generar código desde OpenAPI

El código se genera automáticamente al compilar, pero puedes ejecutarlo manualmente:

```bash
./gradlew buildSpringServer
```

Esto genera:
- Interfaces API (`AccountsApi`, `MovementsApi`, `ReportsApi`)
- Modelos de request/response
- Validaciones Bean Validation

### Regenerar MapStruct Mappers

Los mappers se generan automáticamente en compile-time:

```bash
./gradlew compileJava
```

Los mappers generados están en: `build/generated/sources/annotationProcessor/`

### Hot Reload en Desarrollo

Spring DevTools está habilitado. Los cambios se aplican automáticamente al guardar:

```bash
./gradlew bootRun
```

### Convenciones de Código

#### Nomenclatura

- **Controllers**: `*Controller` (implementan `*Api` de OpenAPI)
- **Use Cases**: `*UseCase` (interfaces) / `*Service` (implementaciones)
- **Repositories**: `*Repository` (interfaces) / `R2dbc*Repository` (implementaciones)
- **Mappers**: `*Mapper` (interfaces MapStruct) / `*MapperImpl` (generadas)
- **Entities**: `*Entity` (persistencia) / `*` (dominio)

#### Paquetes

- `domain.*` - Sin dependencias externas (Java puro)
- `application.*` - Depende solo de domain
- `infrastructure.*` - Depende de application y domain, contiene dependencias de frameworks

### Agregar un Nuevo Endpoint

1. **Actualizar OpenAPI spec**: `src/main/resources/openapi/account-service-api.yaml`
2. **Regenerar código**: `./gradlew buildSpringServer`
3. **Implementar interfaz** en el controller correspondiente
4. **Crear Use Case** si es necesario
5. **Agregar tests**

Ejemplo:

```yaml
# openapi/account-service-api.yaml
paths:
  /accounts/{accountNumber}/balance:
    get:
      operationId: getAccountBalance
      # ... spec completa
```

```java
// AccountController.java
@Override
public Mono<ResponseEntity<BalanceResponse>> getAccountBalance(
    Long accountNumber,
    UUID xRequestId,
    ServerWebExchange exchange
) {
    return getAccountUseCase.getAccount(accountNumber)
        .map(account -> ResponseEntity.ok(
            new BalanceResponse().balance(account.getBalance())
        ));
}
```

### Patrones de Diseño Implementados

- **Hexagonal Architecture (Ports & Adapters)**
- **Repository Pattern** (con R2DBC)
- **Mapper Pattern** (MapStruct)
- **Anti-Corruption Layer** (mappers entre capas)
- **Use Case Pattern** (casos de uso del negocio)
- **Factory Pattern** (creación de entidades)
- **Builder Pattern** (construcción de objetos complejos)
- **Strategy Pattern** (diferentes tipos de movimientos)
- **CQRS** (separación de comandos y consultas)

### Principios SOLID

- **Single Responsibility**: Cada clase tiene una única responsabilidad
- **Open/Closed**: Extensible sin modificar código existente
- **Liskov Substitution**: Implementaciones intercambiables de interfaces
- **Interface Segregation**: Interfaces específicas (Use Cases)
- **Dependency Inversion**: Dependencias hacia abstracciones (ports)

## Integración con Otros Servicios

### api-customer-service

- **Sincronización vía Kafka**: Eventos de creación/actualización de clientes
- **Topic**: `customer-events`

### Flujo de Sincronización

1. `api-customer-service` publica evento en Kafka
2. `CustomerEventConsumer` consume el evento
3. Las cuentas pueden crearse sin llamadas HTTP síncronas

## Gestión de Errores

### Códigos HTTP

- `200 OK` - Operación exitosa
- `201 Created` - Recurso creado
- `400 Bad Request` - Error de validación
- `404 Not Found` - Recurso no encontrado
- `409 Conflict` - Conflicto de concurrencia (versión)
- `422 Unprocessable Entity` - Regla de negocio violada
- `500 Internal Server Error` - Error interno

### Estructura de Errores

```json
{
  "type": "BUSINESS_RULE_VIOLATION",
  "title": "Saldo insuficiente",
  "status": 422,
  "detail": "El saldo actual (100.00) es insuficiente para el retiro de 150.00",
  "instance": "/movements",
  "timestamp": "2025-11-10T14:30:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Monitoreo y Observabilidad

### Actuator Endpoints

```
/actuator/health        - Estado de salud
/actuator/info          - Información de la aplicación
/actuator/metrics       - Métricas
/actuator/prometheus    - Métricas en formato Prometheus
```

### Logging

Configuración de niveles en `application.yml`:

```yaml
logging:
  level:
    com.espiedra.api.account.service: DEBUG
    org.springframework.r2dbc: DEBUG
    org.springframework.kafka: INFO
```

## Contribución

### Workflow de Desarrollo

1. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
2. Desarrollar siguiendo convenciones
3. Ejecutar tests: `./gradlew test`
4. Commit con mensajes descriptivos
5. Push y crear Pull Request
6. Code review
7. Merge a main

### Commit Messages

Seguir convención:

```
tipo(alcance): descripción corta

Descripción detallada (opcional)

BREAKING CHANGE: descripción de cambios incompatibles (si aplica)
```

Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

## Licencia

Este proyecto es parte de una prueba técnica.

## Autor

**Emilio Piedra**
- Email: emiliopiedra2000@gmail.com

## Versión

**1.0.0** - Noviembre 2025

---

**Última actualización**: 10 de Noviembre de 2025
