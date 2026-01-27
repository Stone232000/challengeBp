package com.espiedra.api.account.service.domain.exception;

import java.util.UUID;

/**
 * DOMAIN EXCEPTION: CustomerNotActiveException
 * <p>
 * Thrown when attempting to create an account for an inactive customer.
 * <p>
 * BUSINESS RULE:
 * - Accounts can only be created for active customers (state = TRUE)
 * - Inactive customers cannot perform banking operations
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception as Domain Concept: Business exception in domain layer
 * - Business Rule Validation Pattern
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents inactive customer scenario
 * - Open/Closed: Extends DomainException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 400 BAD REQUEST in REST layer
 * <p>
 * USAGE:
 * - Thrown by CustomerValidationService when customer.state = false
 * - Thrown by CreateAccountUseCase when validating customer eligibility
 *
 * @author Emilio Piedra
 * @version 1.0.0
 */
public class CustomerNotActiveException extends DomainException {

    /**
     * Constructor with custom message
     *
     * @param message Error message describing the inactive customer scenario
     */
    public CustomerNotActiveException(String message) {
        super(message, "CUSTOMER_NOT_ACTIVE");
    }

    /**
     * Constructor with customer ID
     *
     * @param customerId The inactive customer ID
     */
    public CustomerNotActiveException(UUID customerId) {
        super(String.format("Customer with ID %s is not active. Cannot create account for inactive customer.", customerId), "CUSTOMER_NOT_ACTIVE");
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public CustomerNotActiveException(String message, Throwable cause) {
        super(message, "CUSTOMER_NOT_ACTIVE", cause);
    }
}
