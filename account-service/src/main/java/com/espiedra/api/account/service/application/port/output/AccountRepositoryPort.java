package com.espiedra.api.account.service.application.port.output;

import com.espiedra.api.account.service.domain.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * APPLICATION PORT (OUTPUT): AccountRepository
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining infrastructure capability
 * - Repository Pattern: Abstract data access layer
 * - Dependency Inversion Principle (DIP): Domain defines contract, infrastructure implements
 * <p>
 * SOLID PRINCIPLES:
 * - Dependency Inversion: High-level domain doesn't depend on low-level infrastructure
 * - Interface Segregation: Specific interface for account persistence
 * - Single Responsibility: Only defines account data access contract
 * <p>
 * IMPLEMENTATION:
 * - Implemented by AccountR2dbcAdapter in infrastructure layer
 * - Uses R2DBC for reactive database access
 * <p>
 */
public interface AccountRepositoryPort {

    /**
     * Save a new account or update an existing one
     *
     * @param account Account to save
     * @return Mono<Account> Saved account
     */
    Mono<Account> save(Account account);

    /**
     * Find an account by its account number
     *
     * @param accountNumber Account number
     * @return Mono<Account> Account if found, empty Mono otherwise
     */
    Mono<Account> findByAccountNumber(Long accountNumber);

    /**
     * Find all accounts belonging to a customer
     *
     * @param customerId Customer ID
     * @return Flux<Account> Stream of accounts
     */
    Flux<Account> findByCustomerId(UUID customerId);

    /**
     * Find all accounts in the system
     *
     * @return Flux<Account> Stream of all accounts
     */
    Flux<Account> findAll();

    /**
     * Delete an account by account number
     *
     * @param accountNumber Account number to delete
     * @return Mono<Void> Completion signal
     */
    Mono<Void> deleteByAccountNumber(Long accountNumber);

    /**
     * Count active accounts for a customer
     *
     * @param customerId Customer ID
     * @return Mono<Long> Count of active accounts
     */
    Mono<Long> countActiveAccountsByCustomerId(UUID customerId);

    /**
     * Check if a customer already has an active account of a specific type
     *
     * BUSINESS RULE: A customer can only have ONE active account per type
     * - ONE AHORRO (savings) account
     * - ONE CORRIENTE (checking) account
     *
     * @param customerId Customer ID
     * @param accountType Account type (AHORRO or CORRIENTE)
     * @return Mono<Boolean> true if customer already has an active account of this type
     */
    Mono<Boolean> existsActiveAccountByCustomerIdAndAccountType(UUID customerId, String accountType);
}
