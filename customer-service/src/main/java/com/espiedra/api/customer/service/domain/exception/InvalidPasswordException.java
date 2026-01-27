package com.espiedra.api.customer.service.domain.exception;

/**
 * Exception thrown when password validation fails.
 * <p>
 * DESIGN PATTERN: Specific Exception Pattern
 * Used for authentication and password validation scenarios.
 */
public class InvalidPasswordException extends DomainException {

    private static final String ERROR_CODE = "INVALID_PASSWORD";

    /**
     * Default constructor.
     */
    public InvalidPasswordException() {
        super("Invalid password provided", ERROR_CODE);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public InvalidPasswordException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructor with message and cause.
     *
     * @param message Error message
     * @param cause Root cause
     */
    public InvalidPasswordException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}
