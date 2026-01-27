package com.espiedra.api.account.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DOMAIN MODEL: Customer (Value Object)
 * <p>
 * Represents customer data from Customer Service.
 * This is a Value Object in Domain-Driven Design (DDD).
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Value Object Pattern (DDD): Immutable representation of customer data
 * - DTO Pattern: Data transfer from external service
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only stores customer data
 * - Dependency Inversion: Depends on abstraction (Customer exists in another service)
 * <p>
 * PURPOSE:
 * - Data transfer from Customer Service via WebClient
 * - Data transfer from Kafka events (banking.customer.events)
 * - Validation of customer state for business rules
 * <p>
 * DATA SOURCE:
 * - WebClient call to Customer Service (validation)
 * - Kafka events (CUSTOMER_CREATED, CUSTOMER_UPDATED, CUSTOMER_DELETED)
 * <p>
 * NOTE: This is NOT a database entity - it's a pure domain model
 * <p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /**
     * Customer UUID from Customer Service
     * Same ID as in the Customer Service database
     */
    private UUID customerId;

    /**
     * Customer full name
     * Cached for display purposes (account statements, reports)
     */
    private String name;

    /**
     * Customer identification number (DNI, Passport, etc.)
     * Cached for validation and reports
     * UNIQUE constraint ensures no duplicate identifications
     */
    private String identification;

    /**
     * Customer gender (from Customer Service)
     * Cached for display purposes and reports
     */
    private String gender;

    /**
     * Customer age (from Customer Service)
     * Cached for display purposes and reports
     */
    private Integer age;

    /**
     * Customer address (from Customer Service)
     * Cached for display purposes and reports
     */
    private String address;

    /**
     * Customer phone number (from Customer Service)
     * Cached for display purposes and reports
     */
    private String phone;

    /**
     * Customer state: TRUE (active), FALSE (inactive)
     * Used for validation: Cannot create accounts for inactive customers
     */
    private Boolean state;


    // ========================================================================
    // BUSINESS METHODS (Domain Logic)
    // ========================================================================

    /**
     * Business Method: Check if customer is active
     *
     * @return true if state is TRUE (active), false otherwise
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.state);
    }
}
