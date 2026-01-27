package com.espiedra.api.customer.service.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when optimistic locking conflict occurs.
 * <p>
 * DESIGN PATTERN: Optimistic Locking Exception Pattern
 * Indicates concurrent modification conflict.
 */
public class OptimisticLockException extends DomainException {

    private static final String ERROR_CODE = "OPTIMISTIC_LOCK_CONFLICT";

    /**
     * Constructor with entity ID and expected/actual versions.
     *
     * @param entityId Entity ID
     * @param expectedVersion Expected version
     * @param actualVersion Actual version in database
     */
    public OptimisticLockException(UUID entityId, Long expectedVersion, Long actualVersion) {
        super(String.format(
                "Optimistic lock conflict for entity '%s'. Expected version: %d, Actual version: %d",
                entityId, expectedVersion, actualVersion
        ), ERROR_CODE);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public OptimisticLockException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructor with message and cause.
     *
     * @param message Error message
     * @param cause Root cause
     */
    public OptimisticLockException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}
