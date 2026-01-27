package com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.mapper;

import com.espiedra.api.customer.service.domain.model.Customer;
import com.espiedra.api.customer.service.domain.model.GenderType;
import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity.CustomerEntity;
import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity.PersonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct Mapper - Customer Persistence
 * <p>
 * Converts between domain models (Person, Customer) and database entities (PersonEntity, CustomerEntity).
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This mapper is part of the INFRASTRUCTURE layer (Secondary Adapter).
 * It translates between domain (application layer) and persistence (infrastructure layer).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only handles entity ↔ domain mapping
 * - Dependency Inversion Principle (DIP): Domain doesn't depend on entities
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Mapper Pattern: Converts between different object representations
 * - DTO Pattern: Separates domain models from persistence models
 * <p>
 * MapStruct Configuration:
 * - componentModel = "spring": Generates Spring bean
 * - injectionStrategy = "constructor": Uses constructor injection (best practice)
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR
)
public interface CustomerPersistenceMapper {

    /**
     * Convert Customer (which extends Person) to PersonEntity.
     * Maps inherited Person fields from Customer to PersonEntity.
     * Note: isNew field defaults to true via @Builder.Default
     *
     * @param customer Customer domain model (extends Person)
     * @return PersonEntity for database
     */
    @Mapping(target = "isNew", ignore = true)
    @Mapping(source = "personId", target = "personId")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "gender", target = "gender")
    @Mapping(source = "age", target = "age")
    @Mapping(source = "identification", target = "identification")
    @Mapping(source = "address", target = "address")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    PersonEntity customerToPersonEntity(Customer customer);

    /**
     * Convert Customer domain model to CustomerEntity (only customer-specific fields).
     * Note: isNew field defaults to true via @Builder.Default
     * Note: personId is set manually in the adapter after saving person
     *
     * @param customer Customer domain model (extends Person)
     * @return CustomerEntity for database
     */
    @Mapping(target = "isNew", ignore = true)
    @Mapping(target = "personId", ignore = true)
    CustomerEntity customerToEntity(Customer customer);

    /**
     * Combine PersonEntity and CustomerEntity into full Customer domain model.
     * <p>
     * Customer extends Person, so we map both PersonEntity and CustomerEntity fields
     * into a single Customer instance.
     * <p>
     * INHERITANCE MAPPING:
     * - PersonEntity fields → Customer inherited fields (from Person base class)
     * - CustomerEntity fields → Customer specific fields
     *
     * @param personEntity PersonEntity from database
     * @param customerEntity CustomerEntity from database
     * @return Full Customer domain model with all inherited and specific fields
     */
    default Customer toDomain(PersonEntity personEntity, CustomerEntity customerEntity) {
        if (personEntity == null || customerEntity == null) {
            return null;
        }

        // Customer extends Person, so we build a Customer with all fields
        return Customer.builder()
                // Customer-specific fields
                .customerId(customerEntity.getCustomerId())
                .password(customerEntity.getPassword())
                .state(customerEntity.getState())
                .version(customerEntity.getVersion())
                // Inherited Person fields
                .personId(personEntity.getPersonId())
                .name(personEntity.getName())
                .gender(GenderType.valueOf(personEntity.getGender()))
                .age(personEntity.getAge())
                .identification(personEntity.getIdentification())
                .address(personEntity.getAddress())
                .phone(personEntity.getPhone())
                // Timestamps (use CustomerEntity timestamps as source of truth)
                .createdAt(customerEntity.getCreatedAt())
                .updatedAt(customerEntity.getUpdatedAt())
                .build();
    }
}
