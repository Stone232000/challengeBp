package com.espiedra.api.customer.service.domain.exception;

/**
 * Exception thrown when attempting to create a customer that already exists.
 * <p>
 * DESIGN PATTERN: Specific Exception Pattern
 * Indicates a business rule violation (unique constraint).
 */
public class CustomerAlreadyExistsException extends DomainException {

    private static final String ERROR_CODE = "CUSTOMER_ALREADY_EXISTS";

    /**
     * Constructor with identification number.
     *
     * @param identification Customer identification that already exists
     */
    public CustomerAlreadyExistsException(String identification) {
        super(String.format("Customer with identification '%s' already exists", identification), ERROR_CODE);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public CustomerAlreadyExistsException(String message, boolean customMessage) {
        super(message, ERROR_CODE);
    }
}
