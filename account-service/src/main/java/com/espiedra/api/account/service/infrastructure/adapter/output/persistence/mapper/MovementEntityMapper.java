package com.espiedra.api.account.service.infrastructure.adapter.output.persistence.mapper;

import com.espiedra.api.account.service.domain.model.Movement;
import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.entity.MovementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MAPPER: MovementEntityMapper (MapStruct)
 * <p>
 * DESIGN PATTERNS:
 * - Mapper Pattern: Translates between layers
 * - Adapter Pattern: Adapts domain to infrastructure
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only maps Movement <-> MovementEntity
 * - Dependency Inversion: Domain doesn't know about entities
 * <p>
 * TECHNOLOGY:
 * - MapStruct: Compile-time code generation for type-safe mapping
 * - Spring Integration: componentModel = "spring" for DI
 * - Constructor Injection: injectionStrategy = CONSTRUCTOR
 * <p>
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR
)
public interface MovementEntityMapper {

    /**
     * Convert Domain Model to Entity
     * <p>
     * MapStruct automatically maps fields with same names:
     * - movementId, accountNumber, amount, balanceBefore, balanceAfter
     * - description, reference, transactionId, reversedMovementId, reversed
     * - idempotencyKey, createdAt, correlationId, requestId
     * - movementType enum is converted to String automatically
     *
     * @param movement Domain model
     * @return MovementEntity for persistence
     */
    MovementEntity toEntity(Movement movement);

    /**
     * Convert Entity to Domain Model
     * <p>
     * MapStruct automatically maps fields with same names:
     * - movementId, accountNumber, amount, balanceBefore, balanceAfter
     * - description, reference, transactionId, reversedMovementId, reversed
     * - idempotencyKey, createdAt, correlationId, requestId
     * - movementType String is converted to enum automatically
     *
     * @param entity Persistence entity
     * @return Movement domain model
     */
    Movement toDomain(MovementEntity entity);
}
