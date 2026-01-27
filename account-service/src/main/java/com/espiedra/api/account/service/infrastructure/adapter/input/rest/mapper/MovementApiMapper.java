package com.espiedra.api.account.service.infrastructure.adapter.input.rest.mapper;

import com.espiedra.api.account.service.domain.model.Movement;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementCreateRequest;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * MAPPER: MovementApiMapper (MapStruct)
 * <p>
 * Maps between Domain Model (Movement) and OpenAPI Models (MovementCreateRequest, MovementResponse).
 * This is part of the Primary/Driving Adapter in Hexagonal Architecture.
 * <p>
 * DESIGN PATTERNS:
 * - Mapper Pattern: Translates between layers
 * - Adapter Pattern: Adapts API contracts to domain
 * - Anti-Corruption Layer: Protects domain from API changes
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only maps Movement API models
 * - Dependency Inversion: API depends on domain, not vice versa
 * <p>
 * TECHNOLOGY:
 * - MapStruct: Compile-time code generation for type-safe mapping
 * - Spring Integration: componentModel = "spring" for DI
 * - Constructor Injection: injectionStrategy = CONSTRUCTOR
 * <p>
 * TYPE CONVERSIONS:
 * - Double (OpenAPI) <-> BigDecimal (Domain)
 * - OffsetDateTime (OpenAPI) <-> LocalDateTime (Domain)
 * - String (OpenAPI transactionId) <-> UUID (Domain)
 * - UUID (OpenAPI movementId) <-> Long (Domain movementId)
 * <p>
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR
)
public interface MovementApiMapper {

    /**
     * Convert MovementCreateRequest (API) to Movement (Domain)
     * <p>
     * Used when creating a new movement from REST request.
     * movementId, balanceBefore, balanceAfter, transactionId are set by the service layer.
     * <p>
     * ENUM MAPPING:
     * - CREDITO (API) → CREDITO (Domain)
     * - DEBITO (API) → DEBITO (Domain)
     * - REVERSA (API) → REVERSA (Domain)
     *
     * @param request OpenAPI request model
     * @return Movement domain model
     */
    @Mapping(target = "movementId", ignore = true)
    @Mapping(target = "balanceBefore", ignore = true)
    @Mapping(target = "balanceAfter", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "reversedMovementId", ignore = true)
    @Mapping(target = "reversed", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "movementType", source = "movementType", qualifiedByName = "apiToDomainType")
    Movement toDomain(MovementCreateRequest request);

    /**
     * Convert Movement (Domain) to MovementResponse (API)
     * <p>
     * Used when returning movement data to REST client.
     * transactionId (UUID) is converted to String.
     * movementId (Long) is converted to UUID - NOTE: This is a temporary mapping
     * as the domain should ideally use UUID for movementId.
     * <p>
     * ENUM MAPPING:
     * - DEPOSITO (Domain) → CREDITO (API)
     * - RETIRO (Domain) → DEBITO (API)
     *
     * @param movement Domain model
     * @return MovementResponse OpenAPI model
     */
    @Mapping(target = "movementId", source = "movementId")
    @Mapping(target = "transactionId", source = "transactionId")
    @Mapping(target = "reversedMovementId", source = "reversedMovementId")
    @Mapping(target = "reversed", source = "reversed")
    @Mapping(target = "reference", source = "reference")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "movementType", source = "movementType", qualifiedByName = "domainToApiType")
    MovementResponse toResponse(Movement movement);

    /**
     * Convert Double to BigDecimal
     * Handles null values and precision conversion
     */
    default BigDecimal doubleToBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    /**
     * Convert BigDecimal to Double
     * Handles null values and precision conversion
     */
    default Double bigDecimalToDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Convert LocalDateTime to OffsetDateTime
     * Assumes UTC timezone
     */
    default OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    /**
     * Map MovementType from API to Domain
     * CREDITO (API) → CREDITO (Domain)
     * DEBITO (API) → DEBITO (Domain)
     * REVERSA (API) → REVERSA (Domain)
     */
    @Named("apiToDomainType")
    default com.espiedra.api.account.service.domain.model.MovementType apiToDomainType(
            com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementType apiType) {
        if (apiType == null) {
            return null;
        }
        return switch (apiType) {
            case CREDITO -> com.espiedra.api.account.service.domain.model.MovementType.CREDITO;
            case DEBITO -> com.espiedra.api.account.service.domain.model.MovementType.DEBITO;
            case REVERSA -> com.espiedra.api.account.service.domain.model.MovementType.REVERSA;
        };
    }

    /**
     * Map MovementType from Domain to API
     * CREDITO (Domain) → CREDITO (API)
     * DEBITO (Domain) → DEBITO (API)
     * REVERSA (Domain) → REVERSA (API)
     */
    @Named("domainToApiType")
    default com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementType domainToApiType(
            com.espiedra.api.account.service.domain.model.MovementType domainType) {
        if (domainType == null) {
            return null;
        }
        return switch (domainType) {
            case CREDITO -> com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementType.CREDITO;
            case DEBITO -> com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementType.DEBITO;
            case REVERSA -> com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementType.REVERSA;
        };
    }
}
