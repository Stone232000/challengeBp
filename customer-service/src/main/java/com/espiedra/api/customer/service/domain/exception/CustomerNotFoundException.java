package com.espiedra.api.customer.service.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when a customer is not found in the system.
 * <p>
 * DESIGN PATTERN: Specific Exception Pattern
 * This allows precise error handling at different layers.
 */
public class CustomerNotFoundException extends DomainException {

    private static final String ERROR_CODE = "CUSTOMER_NOT_FOUND";

    /**
     * Constructor with customer ID.
     *
     * @param customerId Customer UUID that was not found
     */
    public CustomerNotFoundException(UUID customerId) {
        super(String.format("Customer with ID '%s' not found", customerId), ERROR_CODE);
    }

    /**
     * Constructor with identification number.
     *
     * @param identification Customer identification that was not found
     */
    public CustomerNotFoundException(String identification) {
        super(String.format("Customer with identification '%s' not found", identification), ERROR_CODE);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public CustomerNotFoundException(String message, boolean customMessage) {
        super(message, ERROR_CODE);
    }
}
