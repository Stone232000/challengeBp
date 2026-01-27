package com.espiedra.api.account.service.infrastructure.adapter.output.persistence.mapper;

import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MAPPER: AccountEntityMapper (MapStruct)
 * <p>
 * DESIGN PATTERNS:
 * - Mapper Pattern: Translates between layers
 * - Adapter Pattern: Adapts domain to infrastructure
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only maps Account <-> AccountEntity
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
public interface AccountEntityMapper {

    /**
     * Convert Domain Model to Entity
     * <p>
     * MapStruct automatically maps fields with same names:
     * - accountNumber, customerId, customerName, accountType, balance, state, version
     * - createdAt, updatedAt
     * - accountType enum is converted to String automatically
     * - initialBalance is ignored (not persisted separately, only balance is stored)
     * <p>
     * Spring Data R2DBC determines INSERT vs UPDATE based on accountNumber:
     * - accountNumber == null → INSERT (database generates via sequence)
     * - accountNumber != null → UPDATE
     *
     * @param account Domain model
     * @return AccountEntity for persistence
     */
    AccountEntity toEntity(Account account);

    /**
     * Convert Entity to Domain Model
     * <p>
     * MapStruct automatically maps fields with same names:
     * - accountNumber, customerId, customerName, accountType, balance, state, version
     * - createdAt, updatedAt
     * - accountType String is converted to enum automatically
     * - initialBalance is set from balance (since we don't persist it separately)
     * <p>
     * @param entity Persistence entity
     * @return Account domain model
     */
    @Mapping(target = "initialBalance", source = "balance")
    Account toDomain(AccountEntity entity);
}
