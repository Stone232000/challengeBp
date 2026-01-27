package com.espiedra.api.account.service.domain.exception;

/**
 * DOMAIN EXCEPTION: AccountNotFoundException
 * <p>
 * Thrown when a requested account does not exist in the system.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception as Domain Concept: Business exception in domain layer
 * - Fail-Fast Pattern: Immediate validation and failure
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents account not found scenario
 * - Open/Closed: Extends DomainException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 404 NOT FOUND in REST layer
 * <p>
 * USAGE:
 * - Thrown by repositories when account lookup by ID fails
 * - Thrown by use cases when validating account existence
 * <p>
 */
public class AccountNotFoundException extends DomainException {

    /**
     * Constructor with custom message
     *
     * @param message Error message describing the not found scenario
     */
    public AccountNotFoundException(String message) {
        super(message, "ACCOUNT_NOT_FOUND");
    }

    /**
     * Constructor with account number
     *
     * @param accountNumber The account number that was not found
     */
    public AccountNotFoundException(Long accountNumber) {
        super(String.format("Account with number %d not found", accountNumber), "ACCOUNT_NOT_FOUND");
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, "ACCOUNT_NOT_FOUND", cause);
    }
}
