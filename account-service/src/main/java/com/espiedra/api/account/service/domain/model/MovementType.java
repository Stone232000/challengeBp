package com.espiedra.api.account.service.domain.model;

/**
 * DOMAIN VALUE OBJECT: MovementType
 * <p>
 * Represents the type of transaction movement.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Value Object Pattern (DDD): Immutable, type-safe enumeration
 * - Type-Safe Enum Pattern: Compile-time validation
 * <p>
 * SOLID PRINCIPLES:
 * - Open/Closed: Open for extension (add new types), closed for modification
 * - Single Responsibility: Only defines movement types
 * <p>
 * BUSINESS RULES:
 * - CREDITO: Credit transaction (adds money to account)
 * - DEBITO: Debit transaction (subtracts money from account)
 * - REVERSA: Reversal of a previous transaction
 *
 * @author Emilio Piedra
 * @version 1.0.0
 */
public enum MovementType {
    /**
     * CREDITO: Credit transaction
     * Adds money to the account balance
     * Example: Customer deposits cash or receives a transfer
     */
    CREDITO,

    /**
     * DEBITO: Debit transaction
     * Subtracts money from the account balance
     * Example: Customer withdraws cash or makes a payment
     */
    DEBITO,

    /**
     * REVERSA: Reversal transaction
     * Reverses a previous transaction (CREDITO or DEBITO)
     * Used for corrections, chargebacks, or cancellations
     */
    REVERSA
}
