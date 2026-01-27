package com.espiedra.api.customer.service.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting operations on an inactive customer.
 * <p>
 * DESIGN PATTERN: Specific Exception Pattern
 * Enforces business rule: inactive customers cannot perform certain operations.
 */
public class CustomerInactiveException extends DomainException {

    private static final String ERROR_CODE = "CUSTOMER_INACTIVE";

    /**
     * Constructor with customer ID.
     *
     * @param customerId Customer UUID
     */
    public CustomerInactiveException(UUID customerId) {
        super(String.format("Customer with ID '%s' is inactive", customerId), ERROR_CODE);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public CustomerInactiveException(String message) {
        super(message, ERROR_CODE);
    }
}
