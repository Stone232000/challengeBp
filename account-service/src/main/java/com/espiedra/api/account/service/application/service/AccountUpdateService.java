package com.espiedra.api.account.service.application.service;

import com.espiedra.api.account.service.application.dto.AccountEventDTO;
import com.espiedra.api.account.service.application.port.input.UpdateAccountUseCase;
import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.application.port.output.EventPublisherPort;
import com.espiedra.api.account.service.domain.exception.AccountNotFoundException;
import com.espiedra.api.account.service.domain.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * APPLICATION SERVICE: AccountUpdateService
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Service Pattern: Encapsulates account update operations
 * - Command Pattern: Executes account update commands
 * - Event-Driven Pattern: Publishes events to Kafka
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles account updates
 * - Dependency Inversion: Depends on abstractions (ports)
 * <p>
 * BUSINESS CAPABILITIES:
 * - Update Account (administrative changes)
 * <p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountUpdateService implements UpdateAccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    private static final String ACCOUNT_EVENTS_TOPIC = "banking.account.events";

    /**
     * Update an existing account
     * <p>
     * BUSINESS RULES:
     * - Account must exist
     * - Cannot update balance directly (only via movements)
     * - Can update: accountType, state
     * - Uses optimistic locking (version)
     * <p>
     * PATTERN: Command Pattern - executes account update command
     * PATTERN: Optimistic Locking - version control for concurrency
     * SOLID: Single Responsibility - only updates accounts
     *
     * @param accountNumber Account number to update
     * @param account       Updated account data
     * @return Mono<Account> Updated account
     */
    @Override
    public Mono<Account> updateAccount(Long accountNumber, Account account) {
        log.info("Updating account: accountNumber={}", accountNumber);

        return accountRepositoryPort.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(accountNumber)))
                .flatMap(existingAccount -> {

                    log.debug("Updating account fields: accountType={}, state={}", account.getAccountType(), account.getState());

                    if (account.getAccountType() != null) {
                        existingAccount.setAccountType(account.getAccountType());
                    }
                    if (account.getState() != null) {
                        existingAccount.setState(account.getState());
                    }

                    return accountRepositoryPort.save(existingAccount);
                })
                .doOnSuccess(updatedAccount -> {
                    log.info("Account updated successfully: accountNumber={}, state={}", updatedAccount.getAccountNumber(), updatedAccount.getState());

                    // PATTERN: Fire-and-Forget Event Publishing
                    publishAccountUpdatedEvent(updatedAccount);
                })
                .doOnError(error -> log.error("Error updating account {}: {}", accountNumber, error.getMessage()));
    }

    /**
     * Publish ACCOUNT_UPDATED event to Kafka
     *
     * @param account Updated account
     */
    private void publishAccountUpdatedEvent(Account account) {
        AccountEventDTO event = AccountEventDTO.builder()
                .eventType("account.updated")
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .balance(account.getBalance())
                .state(account.getState())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisherPort.publish(ACCOUNT_EVENTS_TOPIC, account.getAccountNumber().toString(), event)
                .subscribe(
                        v -> log.debug("ACCOUNT_UPDATED event published: accountNumber={}", account.getAccountNumber()),
                        error -> log.error("Error publishing ACCOUNT_UPDATED event: {}", error.getMessage())
                );
    }
}
