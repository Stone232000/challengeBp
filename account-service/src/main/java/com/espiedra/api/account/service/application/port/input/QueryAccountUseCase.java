package com.espiedra.api.account.service.application.port.input;

import com.espiedra.api.account.service.domain.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * APPLICATION PORT (INPUT): QueryAccountUseCase
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining business capability
 * - Query Pattern: Read-only operations
 * - Interface Segregation Principle (ISP): Single interface for related queries
 * <p>
 * SOLID PRINCIPLES:
 * - Interface Segregation: Clients depend only on query operations
 * - Dependency Inversion: Domain depends on abstraction, not concrete implementation
 * - Single Responsibility: Only defines account query contracts
 * <p>
 * IMPLEMENTATION:
 * - Implemented by AccountService in application layer
 * - Called by REST controllers (primary adapters)
 * <p>
 * USE CASES:
 * - Get account by account number
 * - Get all accounts for a customer
 * - Get all accounts in the system
 * <p>
 */
public interface QueryAccountUseCase {

    /**
     * Get an account by its account number
     * <p>
     * @param accountNumber Account number to retrieve
     * @return Mono<Account> Account if found
     * <p>
     */
    Mono<Account> getAccount(Long accountNumber);

    /**
     * Get all accounts belonging to a customer
     *
     * @param customerId Customer ID
     * @return Flux<Account> Stream of accounts (may be empty)
     */
    Flux<Account> getAccountsByCustomer(UUID customerId);

    /**
     * Get all accounts in the system
     *
     * @return Flux<Account> Stream of all accounts (may be empty)
     */
    Flux<Account> getAllAccounts();
}
