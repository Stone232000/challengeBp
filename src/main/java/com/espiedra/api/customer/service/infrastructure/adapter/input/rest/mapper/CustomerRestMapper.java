package com.espiedra.api.customer.service.infrastructure.adapter.input.rest.mapper;

import com.espiedra.api.customer.service.domain.model.Customer;
import com.espiedra.api.customer.service.infrastructure.input.adapter.rest.customer.service.models.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * MapStruct Mapper - REST DTOs to Domain Models
 * <p>
 * Converts between OpenAPI generated DTOs and domain models (Person, Customer).
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This mapper is part of the INFRASTRUCTURE layer (Primary Adapter).
 * It translates between REST DTOs (OpenAPI generated) and domain models (application layer).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only handles REST DTO ↔ domain mapping
 * - Dependency Inversion Principle (DIP): Controllers depend on domain, not DTOs
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Mapper Pattern: Converts between different object representations
 * - DTO Pattern: Separates REST API contracts from domain models
 * - Adapter Pattern: Adapts OpenAPI DTOs to domain layer
 * <p>
 * MapStruct Configuration:
 * - componentModel = "spring": Generates Spring bean
 * - injectionStrategy = "constructor": Uses constructor injection (best practice)
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR
)
public interface CustomerRestMapper {

    /**
     * Convert CustomerCreateRequest to Customer domain model.
     * <p>
     * Customer extends Person, so we map Person fields directly to Customer's inherited fields.
     *
     * @param request CustomerCreateRequest from OpenAPI
     * @return Customer domain model
     */
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "password", ignore = true) // Password will be set separately after encoding
    @Mapping(target = "state", source = "state", defaultValue = "true")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer requestToCustomer(CustomerCreateRequest request);

    /**
     * Convert CustomerUpdateRequest to Customer domain model.
     * <p>
     * Customer extends Person, so we map Person fields directly to Customer's inherited fields.
     *
     * @param request CustomerUpdateRequest from OpenAPI
     * @return Customer domain model
     */
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "identification", ignore = true) // Identification cannot be updated
    @Mapping(target = "password", ignore = true) // Password is optional, handled separately
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer updateRequestToCustomer(CustomerUpdateRequest request);

    /**
     * Convert Customer domain model to CustomerResponse DTO.
     * <p>
     * Customer extends Person, so Person fields are inherited directly.
     *
     * @param customer Customer domain model
     * @return CustomerResponse for OpenAPI
     */
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "age", source = "age")
    @Mapping(target = "identification", source = "identification")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "token", ignore = true) // Token will be set manually if needed
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    CustomerResponse customerToResponse(Customer customer);

    /**
     * Convert Customer domain model to CustomerValidationResponse DTO.
     * <p>
     * Customer extends Person, so Person fields are inherited directly.
     *
     * @param customer Customer domain model
     * @return CustomerValidationResponse for OpenAPI
     */
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "identification", source = "identification")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "valid", source = "state") // valid = state (active/inactive)
    CustomerValidationResponse customerToValidationResponse(Customer customer);

    /**
     * Convert Customer domain model to CustomerResponse with JWT token.
     * <p>
     * This is used after creating a new customer.
     *
     * @param customer Customer domain model
     * @param token JWT token string
     * @return CustomerResponse with token
     */
    default CustomerResponse customerToResponseWithToken(Customer customer, String token) {
        CustomerResponse response = customerToResponse(customer);
        response.setToken(token);
        return response;
    }

    /**
     * Convert LocalDateTime to OffsetDateTime.
     * <p>
     * MapStruct uses this method automatically for date conversions.
     *
     * @param localDateTime LocalDateTime from domain
     * @return OffsetDateTime for OpenAPI DTOs
     */
    default OffsetDateTime map(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
    }
}
