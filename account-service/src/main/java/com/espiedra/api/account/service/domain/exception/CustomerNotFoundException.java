package com.espiedra.api.account.service.domain.exception;

import java.util.UUID;

/**
 * DOMAIN EXCEPTION: CustomerNotFoundException
 * <p>
 * Thrown when a customer does not exist or cannot be validated.
 * This occurs when:
 * 1. Customer not found in Customer Service (via WebClient)
 * 2. Customer not found in local cache (Kafka)
 * 3. Both validation strategies fail
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception as Domain Concept: Business exception in domain layer
 * - Fail-Fast Pattern: Immediate validation and failure
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents customer not found scenario
 * - Open/Closed: Extends DomainException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 404 NOT FOUND in REST layer
 * <p>
 * USAGE:
 * - Thrown by CustomerValidationService when customer doesn't exist
 * - Thrown by CreateAccountUseCase when pre-validating customer
 * <p>
 */
public class CustomerNotFoundException extends DomainException {

    /**
     * Constructor with custom message
     *
     * @param message Error message describing the not found scenario
     */
    public CustomerNotFoundException(String message) {
        super(message, "CUSTOMER_NOT_FOUND");
    }

    /**
     * Constructor with customer ID
     *
     * @param customerId The customer ID that was not found
     */
    public CustomerNotFoundException(UUID customerId) {
        super(String.format("Customer with ID %s not found or could not be validated", customerId), "CUSTOMER_NOT_FOUND");
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, "CUSTOMER_NOT_FOUND", cause);
    }
}
