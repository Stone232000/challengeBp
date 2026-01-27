package com.espiedra.api.account.service.application.service;

import com.espiedra.api.account.service.application.dto.AccountEventDTO;
import com.espiedra.api.account.service.application.port.input.DeleteAccountUseCase;
import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.application.port.output.EventPublisherPort;
import com.espiedra.api.account.service.application.port.output.MovementRepositoryPort;
import com.espiedra.api.account.service.domain.exception.AccountNotFoundException;
import com.espiedra.api.account.service.domain.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * APPLICATION SERVICE: AccountDeletionService
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Service Pattern: Encapsulates account deletion operations
 * - Command Pattern: Executes account deletion commands
 * - Cascade Delete Pattern: Removes dependent entities
 * - Event-Driven Pattern: Publishes events to Kafka
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles account deletions
 * - Dependency Inversion: Depends on abstractions (ports)
 * <p>
 * BUSINESS CAPABILITIES:
 * - Delete Account (hard delete - permanent removal)
 * - Delete all Accounts for a Customer (cascade delete)
 * <p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountDeletionService implements DeleteAccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final MovementRepositoryPort movementRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    private static final String ACCOUNT_EVENTS_TOPIC = "banking.account.events";

    /**
     * Delete an account (hard delete - permanent removal)
     * <p>
     * BUSINESS RULES:
     * - Account must exist
     * - Balance must be 0 (cannot delete account with funds)
     * - Cascade delete all associated movements
     * - Publish ACCOUNT_DELETED event
     * <p>
     * PATTERN: Command Pattern - executes account deletion command
     * PATTERN: Cascade Delete - remove dependent entities first
     * SOLID: Single Responsibility - only deletes accounts
     *
     * @param accountNumber Account number to delete
     * @return Mono<Void> Completion signal
     */
    @Override
    public Mono<Void> deleteAccount(Long accountNumber) {
        log.info("Deleting account: accountNumber={}", accountNumber);

        return accountRepositoryPort.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(accountNumber)))
                .flatMap(account -> {
                    // BUSINESS RULE: Cannot delete account with balance
                    if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                        log.warn("Cannot delete account {} with non-zero balance: {}", accountNumber, account.getBalance());
                        return Mono.error(new IllegalStateException(
                                String.format("Cannot delete account with non-zero balance. Current balance: %s", account.getBalance())));
                    }

                    log.debug("Account balance is zero, proceeding with deletion");

                    // PATTERN: Cascade Delete - delete movements first
                    return movementRepositoryPort.deleteByAccountNumber(accountNumber)
                            .doOnSuccess(v -> log.debug("Movements deleted for account: {}", accountNumber))
                            .then(accountRepositoryPort.deleteByAccountNumber(accountNumber))
                            .doOnSuccess(v -> log.info("Account deleted successfully: accountNumber={}", accountNumber))
                            .then(Mono.fromRunnable(() -> publishAccountDeletedEvent(account)))
                            .then();
                })
                .doOnError(error -> log.error("Error deleting account {}: {}",
                        accountNumber, error.getMessage()));
    }

    /**
     * Delete all accounts for a customer (cascade delete movements)
     * <p>
     * BUSINESS FLOW:
     * 1. Find all accounts for the customer
     * 2. For each account:
     * - Delete associated movements (cascade)
     * - Delete account record
     * - Publish ACCOUNT_DELETED event to Kafka
     * 3. Return count of deleted accounts
     * <p>
     * USE CASE:
     * - Called from Kafka consumer when CUSTOMER_DELETED event is received
     * - Maintains referential integrity across microservices
     * - NO BALANCE CHECK - force delete even with balance
     * <p>
     * PATTERN: Cascade Delete Pattern
     * SOLID: Single Responsibility - only deletes accounts for customer
     *
     * @param customerId Customer ID (UUID)
     * @return Mono<Long> Number of accounts deleted
     */
    @Override
    public Mono<Long> deleteAccountsByCustomerId(UUID customerId) {
        log.info("Deleting all accounts for customer: customerId={}", customerId);

        return accountRepositoryPort.findByCustomerId(customerId)
                .collectList()
                .flatMap(accounts -> {
                    if (accounts.isEmpty()) {
                        log.info("No accounts found for customer: customerId={}", customerId);
                        return Mono.just(0L);
                    }

                    log.info("Found {} accounts for customer: customerId={}", accounts.size(), customerId);

                    // Delete each account with its movements
                    return Flux.fromIterable(accounts)
                            .flatMap(account -> {
                                Long accountNumber = account.getAccountNumber();
                                log.debug("Deleting account: accountNumber={}, balance={}",
                                        accountNumber, account.getBalance());

                                // PATTERN: Cascade Delete - delete movements first, then account
                                return movementRepositoryPort.deleteByAccountNumber(accountNumber)
                                        .doOnSuccess(v -> log.debug("Movements deleted for account by Number: {}", accountNumber))
                                        .then(accountRepositoryPort.deleteByAccountNumber(accountNumber))
                                        .doOnSuccess(v -> log.debug("Account deleted: accountNumber={}", accountNumber))
                                        .then(Mono.fromRunnable(() -> publishAccountDeletedEvent(account)))
                                        .thenReturn(1L);
                            })
                            .reduce(0L, Long::sum)
                            .doOnSuccess(count -> log.info("Successfully deleted {} accounts for customer: customerId={}", count, customerId))
                            .doOnError(error -> log.error("Error deleting accounts for customer {}: {}", customerId, error.getMessage()));
                });
    }

    /**
     * Publish ACCOUNT_DELETED event to Kafka
     *
     * @param account Deleted account
     */
    private void publishAccountDeletedEvent(Account account) {
        AccountEventDTO event = AccountEventDTO.builder()
                .eventType("account.deleted")
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisherPort.publish(ACCOUNT_EVENTS_TOPIC, account.getAccountNumber().toString(), event)
                .subscribe(
                        v -> log.debug("ACCOUNT_DELETED event published: accountNumber={}", account.getAccountNumber()),
                        error -> log.error("Error publishing ACCOUNT_DELETED event: {}", error.getMessage())
                );
    }
}
