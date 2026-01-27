package com.espiedra.api.account.service.domain.exception;

import lombok.Getter;

/**
 * Base Domain Exception
 * <p>
 * Parent class for all domain-specific exceptions.
 * This follows the Exception Hierarchy Pattern for clean exception handling.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Open/Closed Principle (OCP): Open for extension (subclasses), closed for modification
 * - Liskov Substitution Principle (LSP): All domain exceptions can substitute this base class
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception Hierarchy Pattern: Provides common behavior for all domain exceptions
 */
@Getter
public abstract class DomainException extends RuntimeException {

    /**
     * Error code for categorizing exceptions.
     * -- GETTER --
     *  Get the error code.
     *
     */
    private final String errorCode;

    /**
     * Constructor with message only.
     *
     * @param message Error message
     */
    protected DomainException(String message) {
        super(message);
        this.errorCode = this.getClass().getSimpleName();
    }

    /**
     * Constructor with message and error code.
     *
     * @param message Error message
     * @param errorCode Specific error code
     */
    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor with message and cause.
     *
     * @param message Error message
     * @param cause Root cause
     */
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = this.getClass().getSimpleName();
    }

    /**
     * Constructor with message, error code, and cause.
     *
     * @param message Error message
     * @param errorCode Specific error code
     * @param cause Root cause
     */
    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}
