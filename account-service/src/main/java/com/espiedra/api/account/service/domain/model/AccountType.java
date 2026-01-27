package com.espiedra.api.account.service.domain.model;

/**
 * DOMAIN VALUE OBJECT: AccountType
 * <p>
 * Represents the type of bank account.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Value Object Pattern (DDD): Immutable, type-safe enumeration
 * - Type-Safe Enum Pattern: Compile-time validation
 * <p>
 * SOLID PRINCIPLES:
 * - Open/Closed: Open for extension (add new types), closed for modification
 * - Single Responsibility: Only defines account types
 * <p>
 * BUSINESS RULES:
 * - AHORRO: Savings account (typically earns interest)
 * - CORRIENTE: Checking account (for daily transactions)
 * <p>
 */
public enum AccountType {
    /**
     * AHORRO: Savings account
     * Typically used for saving money and may earn interest
     */
    AHORRO,

    /**
     * CORRIENTE: Checking account
     * Typically used for daily transactions (deposits, withdrawals, transfers)
     */
    CORRIENTE
}
