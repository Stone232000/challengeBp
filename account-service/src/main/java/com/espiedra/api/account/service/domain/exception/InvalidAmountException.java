package com.espiedra.api.account.service.domain.exception;

import java.math.BigDecimal;

/**
 * DOMAIN EXCEPTION: InvalidAmountException
 * <p>
 * Thrown when a transaction amount is invalid (negative, zero, or null).
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception as Domain Concept: Business exception in domain layer
 * - Fail-Fast Pattern: Immediate validation and failure
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents invalid amount scenario
 * - Open/Closed: Extends RuntimeException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 400 BAD REQUEST in REST layer
 * <p>
 * USAGE:
 * - Thrown by Account.credit() / Account.debit() when amount <= 0
 * - Thrown by CreateMovementUseCase when validating input
 * <p>
 * BUSINESS RULE:
 * - Transaction amounts must be positive (> 0)
 * - Null amounts are not allowed
 * <p>
 */
public class InvalidAmountException extends DomainException {

    /**
     * Constructor with custom message
     *
     * @param message Error message describing the invalid amount scenario
     */
    public InvalidAmountException(String message) {
        super(message, "INVALID_AMOUNT");
    }

    /**
     * Constructor with amount value
     *
     * @param amount The invalid amount
     */
    public InvalidAmountException(BigDecimal amount) {
        super(String.format("Invalid transaction amount: %s. Amount must be positive (> 0).", amount),
              "INVALID_AMOUNT");
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public InvalidAmountException(String message, Throwable cause) {
        super(message, "INVALID_AMOUNT", cause);
    }
}
