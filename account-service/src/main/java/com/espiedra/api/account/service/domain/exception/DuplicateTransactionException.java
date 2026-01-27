package com.espiedra.api.account.service.domain.exception;

/**
 * DOMAIN EXCEPTION: DuplicateTransactionException
 * <p>
 * Thrown when attempting to create a movement with a transaction ID that already exists.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Idempotency Pattern: Prevents duplicate transactions
 * - Exception as Domain Concept: Business exception in domain layer
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents duplicate transaction scenario
 * - Open/Closed: Extends RuntimeException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 409 CONFLICT in REST layer
 * <p>
 * USAGE:
 * - Thrown by CreateMovementUseCase when transaction ID already exists
 * - Ensures idempotency in distributed systems
 * <p>
 * BUSINESS RULE:
 * - Transaction IDs must be unique across all movements
 * - Prevents accidental duplicate withdrawals/deposits
 * <p>
 */
public class DuplicateTransactionException extends DomainException {

    /**
     * Constructor with transaction ID
     *
     * @param transactionId The duplicate transaction ID
     */
    public DuplicateTransactionException(String transactionId) {
        super(String.format("Transaction with ID %s already exists. Duplicate transactions are not allowed.", transactionId),
              "DUPLICATE_TRANSACTION");
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public DuplicateTransactionException(String message, Throwable cause) {
        super(message, "DUPLICATE_TRANSACTION", cause);
    }
}
