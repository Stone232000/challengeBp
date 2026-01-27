package com.espiedra.api.account.service.application.service;

import com.espiedra.api.account.service.application.port.input.QueryAccountUseCase;
import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.domain.exception.AccountNotFoundException;
import com.espiedra.api.account.service.domain.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * APPLICATION SERVICE: AccountQueryService
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Service Pattern: Encapsulates account query operations
 * - Query Pattern: Read-only operations (CQRS principle)
 * - Repository Pattern: Uses repository for data access
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles account queries
 * - Dependency Inversion: Depends on abstractions (ports)
 * <p>
 * BUSINESS CAPABILITIES:
 * - Get Account by number
 * - Get Accounts by customer
 * - Get all Accounts
 * <p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountQueryService implements QueryAccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    /**
     * Get an account by its account number
     * <p>
     * PATTERN: Query Pattern - read-only operation
     * SOLID: Single Responsibility - only retrieves account
     *
     * @param accountNumber Account number to retrieve
     * @return Mono<Account> Account if found
     */
    @Override
    public Mono<Account> getAccount(Long accountNumber) {
        log.debug("Retrieving account: accountNumber={}", accountNumber);

        return accountRepositoryPort.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(accountNumber)))
                .doOnSuccess(account -> log.debug("Account retrieved: accountNumber={}, customerId={}, balance={}",
                        account.getAccountNumber(), account.getCustomerId(), account.getBalance()))
                .doOnError(error -> log.error("Error retrieving account {}: {}", accountNumber, error.getMessage()));
    }

    /**
     * Get all accounts belonging to a customer
     * <p>
     * PATTERN: Query Pattern - read-only operation
     * SOLID: Single Responsibility - only retrieves customer accounts
     *
     * @param customerId Customer ID
     * @return Flux<Account> Stream of accounts (may be empty)
     */
    @Override
    public Flux<Account> getAccountsByCustomer(UUID customerId) {
        log.debug("Retrieving accounts for customer: {}", customerId);

        return accountRepositoryPort.findByCustomerId(customerId)
                .doOnComplete(() -> log.debug("Finished retrieving accounts for customer: {}", customerId))
                .doOnError(error -> log.error("Error retrieving accounts for customer {}: {}", customerId, error.getMessage()));
    }

    /**
     * Get all accounts in the system
     * <p>
     * PATTERN: Query Pattern - read-only operation
     * SOLID: Single Responsibility - only retrieves all accounts
     * <p>
     * USE CASES:
     * - Admin dashboard showing all accounts
     * - Global reports
     * - System monitoring
     *
     * @return Flux<Account> Stream of all accounts (may be empty)
     */
    @Override
    public Flux<Account> getAllAccounts() {
        log.debug("Retrieving all accounts");

        return accountRepositoryPort.findAll()
                .doOnComplete(() -> log.debug("Finished retrieving all accounts"))
                .doOnError(error -> log.error("Error retrieving all accounts: {}", error.getMessage()));
    }
}
