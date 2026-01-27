package com.espiedra.api.account.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DOMAIN MODEL: Account (Aggregate Root)
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Aggregate Root Pattern (DDD): Manages account business logic and movements
 * - Persistable Pattern: Controls INSERT vs UPDATE behavior in R2DBC
 * - Builder Pattern: Fluent object construction
 * - Value Object Pattern: Uses immutable AccountType enum
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only manages account state and business rules
 * - Open/Closed: Open for extension (new account types) via enum, closed for modification
 * - Liskov Substitution: Can be substituted by any Account type
 * <p>
 * BUSINESS RULES:
 * - Initial balance must be >= 0
 * - Balance can go negative (overdraft) up to -10,000
 * - Account type must be AHORRO or CORRIENTE
 * - State tracks if account is active/inactive
 * <p>
 * OPTIMISTIC LOCKING:
 * - Uses @Version for concurrent update detection
 * - Version auto-increments on UPDATE via database trigger
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    /**
     * Primary Key: Auto-generated account number
     * Uses BIGSERIAL in PostgreSQL (auto-increment)
     */
    private Long accountNumber;

    /**
     * Foreign Key: Reference to customer (from Customer Service)
     * This is NOT a direct FK in database to avoid tight coupling
     * Customer existence validated via WebClient or Kafka cache
     */
    private UUID customerId;

    /**
     * Customer name (denormalized for performance)
     * Cached from Customer Service to avoid frequent lookups
     */
    private String customerName;

    /**
     * Account type: AHORRO (savings) or CORRIENTE (checking)
     * Uses enum for type safety and validation
     */
    private AccountType accountType;

    /**
     * Initial balance when account was created
     * Immutable after creation (only set on INSERT)
     * Must be >= 0
     */
    private BigDecimal initialBalance;

    /**
     * Current balance (updated by movements)
     * Can be negative (overdraft allowed up to -10,000)
     */
    private BigDecimal balance;

    /**
     * Account state: TRUE (active), FALSE (inactive/closed)
     * Soft delete pattern: PATCH sets state=false, DELETE removes record
     */
    private Boolean state;

    /**
     * Optimistic Locking: Version number
     * Auto-incremented by database trigger on UPDATE
     * Used to detect concurrent modifications
     */
    private Integer version;

    /**
     * Audit timestamp: When account was created
     * Auto-set by database DEFAULT CURRENT_TIMESTAMP
     */
    private LocalDateTime createdAt;

    /**
     * Audit timestamp: When account was last updated
     * Auto-updated by database trigger BEFORE UPDATE
     */
    private LocalDateTime updatedAt;


    // ========================================================================
    // BUSINESS METHODS (Domain Logic)
    // ========================================================================

    /**
     * Business Method: Credit (add money to account)
     * <p>
     * PATTERN: Domain Logic in Aggregate Root
     * SOLID: Single Responsibility - Account manages its own balance
     *
     * @param amount Amount to credit (must be positive)
     * @return Updated balance after credit
     * @throws IllegalArgumentException if amount is <= 0
     */
    public BigDecimal credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        return this.balance;
    }

    /**
     * Business Method: Debit (subtract money from account)
     * <p>
     * PATTERN: Domain Logic in Aggregate Root
     * BUSINESS RULE: Allow overdraft up to -10,000
     *
     * @param amount Amount to debit (must be positive)
     * @return Updated balance after debit
     * @throws IllegalArgumentException if amount is <= 0
     * @throws IllegalStateException if debit would exceed overdraft limit
     */
    public BigDecimal debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        BigDecimal newBalance = this.balance.subtract(amount);
        BigDecimal overdraftLimit = new BigDecimal("-10000.00");

        if (newBalance.compareTo(overdraftLimit) < 0) {
            throw new IllegalStateException(
                String.format("Insufficient balance. Available: %s, Requested: %s, Overdraft limit: %s",
                    this.balance, amount, overdraftLimit)
            );
        }

        this.balance = newBalance;
        return this.balance;
    }

    /**
     * Business Method: Check if account has sufficient balance
     *
     * @param amount Amount to check
     * @return true if balance >= amount, false otherwise
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    /**
     * Business Method: Check if account is active
     *
     * @return true if state is TRUE (active), false otherwise
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.state);
    }

    /**
     * Business Method: Activate account
     */
    public void activate() {
        this.state = true;
    }

    /**
     * Business Method: Deactivate account (soft delete)
     */
    public void deactivate() {
        this.state = false;
    }

    /**
     * Business Method: Get available balance
     *
     * For now, available balance equals current balance.
     * Future enhancement: Could consider overdraft limits, pending transactions, etc.
     *
     * @return Available balance
     */
    public BigDecimal getAvailableBalance() {
        return this.balance;
    }
}
