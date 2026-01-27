package com.espiedra.api.account.service.domain.exception;

/**
 * DOMAIN EXCEPTION: AccountNotActiveException
 * <p>
 * Thrown when attempting to perform operations on an inactive account.
 * <p>
 * BUSINESS RULE:
 * - Movements can only be created for active accounts (state = TRUE)
 * - Inactive accounts are frozen and cannot have transactions
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception as Domain Concept: Business exception in domain layer
 * - Business Rule Validation Pattern
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents inactive account scenario
 * - Open/Closed: Extends DomainException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 400 BAD REQUEST in REST layer
 * <p>
 * USAGE:
 * - Thrown by CreateMovementUseCase when account.state = false
 * - Thrown by any operation requiring an active account
 * <p>
 */
public class AccountNotActiveException extends DomainException {

    /**
     * Constructor with custom message
     *
     * @param message Error message describing the inactive account scenario
     */
    public AccountNotActiveException(String message) {
        super(message, "ACCOUNT_NOT_ACTIVE");
    }

    /**
     * Constructor with account number
     *
     * @param accountNumber The inactive account number
     */
    public AccountNotActiveException(Long accountNumber) {
        super(String.format("Account %d is not active. Cannot perform transactions on inactive accounts.", accountNumber), "ACCOUNT_NOT_ACTIVE");
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public AccountNotActiveException(String message, Throwable cause) {
        super(message, "ACCOUNT_NOT_ACTIVE", cause);
    }
}
