package com.espiedra.api.account.service.domain.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * DOMAIN EXCEPTION: InsufficientBalanceException
 * <p>
 * Thrown when a withdrawal/debit operation would exceed the account's available balance
 * (including overdraft limit).
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception as Domain Concept: Business exception in domain layer
 * - Fail-Fast Pattern: Immediate validation and failure
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents insufficient balance scenario
 * - Open/Closed: Extends RuntimeException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 400 BAD REQUEST in REST layer
 * <p>
 * USAGE:
 * - Thrown by Account.debit() when balance check fails
 * - Thrown by CreateMovementUseCase when validating withdrawal
 *
 * @author Emilio Piedra
 * @version 1.0.0
 */
@Getter
public class InsufficientBalanceException extends DomainException {

    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;
    private final BigDecimal overdraftLimit;

    /**
     * Constructor with custom message
     *
     * @param message Error message describing the insufficient balance scenario
     */
    public InsufficientBalanceException(String message) {
        super(message, "INSUFFICIENT_BALANCE");
        this.currentBalance = null;
        this.requestedAmount = null;
        this.overdraftLimit = null;
    }

    /**
     * Constructor with balance details
     *
     * @param currentBalance Current account balance
     * @param requestedAmount Amount requested to withdraw
     * @param overdraftLimit Maximum overdraft allowed
     */
    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requestedAmount, BigDecimal overdraftLimit) {
        super(String.format(
            "Insufficient balance. Current: %s, Requested: %s, Available (with overdraft): %s",
            currentBalance,
            requestedAmount,
            currentBalance.add(overdraftLimit)
        ), "INSUFFICIENT_BALANCE");
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.overdraftLimit = overdraftLimit;
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, "INSUFFICIENT_BALANCE", cause);
        this.currentBalance = null;
        this.requestedAmount = null;
        this.overdraftLimit = null;
    }

    // Getters for balance details (useful for error responses)

}
