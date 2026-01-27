package com.espiedra.api.account.service.application.service;

import com.espiedra.api.account.service.application.dto.AccountEventDTO;
import com.espiedra.api.account.service.application.port.input.CreateAccountUseCase;
import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.application.port.output.CustomerServiceClientPort;
import com.espiedra.api.account.service.application.port.output.EventPublisherPort;
import com.espiedra.api.account.service.domain.exception.CustomerNotActiveException;
import com.espiedra.api.account.service.domain.exception.CustomerNotFoundException;
import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.domain.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SERVICE: Account Creation Service
 * <p>
 * RESPONSIBILITY: Create new bank accounts
 * <p>
 * DESIGN PATTERNS:
 * - Service Pattern: Encapsulates account creation logic
 * - Single Responsibility: Only handles account creation
 * <p>
 * BUSINESS RULES:
 * - Customer must exist and be active
 * - Customer can have max 5 active accounts
 * - Customer can have only ONE account per type (AHORRO/CORRIENTE)
 * - Initial balance must be >= 0
 * <p>
 * CLEAN CODE REFACTORING:
 * Extracted from AccountService.java to improve:
 * - Single Responsibility Principle
 * - Reduced class size (was 1023 lines)
 * - Improved testability
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountCreationService implements CreateAccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final CustomerServiceClientPort customerServiceClientPort;
    private final EventPublisherPort eventPublisherPort;

    private static final String ACCOUNT_EVENTS_TOPIC = "banking.account.events";
    private static final int MAX_ACTIVE_ACCOUNTS_PER_CUSTOMER = 5;

    /**
     * Create a new bank account for a customer
     * <p>
     * BUSINESS FLOW:
     * 1. Validate customer exists and is active (via Customer Service)
     * 2. Check customer doesn't exceed max active accounts (5)
     * 3. Check customer doesn't have duplicate account type
     * 4. Create account in database
     * 5. Publish AccountCreated event to Kafka
     *
     * @param account Account to create (customerId, accountType, initialBalance required)
     * @return Mono<Account> Created account with generated accountNumber
     * @throws CustomerNotFoundException  If customer doesn't exist
     * @throws CustomerNotActiveException If customer is inactive
     * @throws IllegalStateException      If business rules violated
     */
    @Override
    public Mono<Account> createAccount(Account account) {
        log.info("Creating account: customerId={}, type={}, initialBalance={}",
                account.getCustomerId(), account.getAccountType(), account.getInitialBalance());

        return validateCustomer(account.getCustomerId())
                .flatMap(customer -> validateAccountBusinessRules(account, customer))
                .flatMap(accountRepositoryPort::save)
                .doOnSuccess(created -> {
                    log.info("Account created successfully: accountNumber={}, customerId={}",
                            created.getAccountNumber(), created.getCustomerId());
                    publishAccountCreatedEvent(created);
                })
                .doOnError(error -> log.error("Error creating account for customerId {}: {}",
                        account.getCustomerId(), error.getMessage()));
    }

    /**
     * Create account WITHOUT customer validation
     * <p>
     * USED BY: Kafka consumer when customer.created event received
     * We skip validation because customer was just created in Customer Service
     *
     * @param account Account to create
     * @return Mono<Account> Created account
     */
    @Override
    public Mono<Account> createAccountWithoutCustomerValidation(Account account) {
        log.info("Creating account without validation: customerId={}, type={}",
                account.getCustomerId(), account.getAccountType());

        return accountRepositoryPort.save(account)
                .doOnSuccess(created -> {
                    log.info("Account created (no validation): accountNumber={}", created.getAccountNumber());
                    publishAccountCreatedEvent(created);
                })
                .doOnError(error -> log.error("Error creating account: {}", error.getMessage()));
    }

    // ========================================================================
    // PRIVATE: Validation Methods
    // ========================================================================

    /**
     * Validate customer exists and is active
     * <p>
     * SEPARATION OF CONCERNS:
     * - CustomerWebClientPort (Infrastructure layer) handles:
     *   - Circuit Breaker errors → ServiceUnavailableException
     *   - Timeout errors → ServiceUnavailableException
     *   - Connection errors → ServiceUnavailableException
     * <p>
     * - AccountCreationService (Application layer) only handles:
     *   - Business validation (customer exists, customer active)
     *   - All infrastructure concerns are encapsulated in the adapter
     * <p>
     * EXCEPTIONS PROPAGATED:
     * - CustomerNotFoundException (404)
     * - CustomerNotActiveException (400)
     * - ServiceUnavailableException (503) - from adapter
     */
    private Mono<Customer> validateCustomer(UUID customerId) {
        return customerServiceClientPort.validateCustomer(customerId)
                .doOnSuccess(customer -> log.debug("Customer validated: customerId={}, name={}",
                        customerId, customer.getName()));
    }

    /**
     * Validate account business rules
     */
    private Mono<Account> validateAccountBusinessRules(Account account, Customer customer) {
        return validateMaxActiveAccounts(account.getCustomerId())
                .then(validateDuplicateAccountType(account.getCustomerId(), account.getAccountType().name()))
                .then(Mono.defer(() -> {
                    // Set customer name from validated customer
                    account.setCustomerName(customer.getName());
                    account.setBalance(account.getInitialBalance());
                    account.setState(true);
                    account.setCreatedAt(LocalDateTime.now());
                    return Mono.just(account);
                }));
    }

    /**
     * Validate customer doesn't exceed max active accounts
     */
    private Mono<Void> validateMaxActiveAccounts(UUID customerId) {
        return accountRepositoryPort.countActiveAccountsByCustomerId(customerId)
                .flatMap(count -> {
                    if (count >= MAX_ACTIVE_ACCOUNTS_PER_CUSTOMER) {
                        log.warn("Customer {} exceeded max active accounts: {}/{}",
                                customerId, count, MAX_ACTIVE_ACCOUNTS_PER_CUSTOMER);
                        return Mono.error(new IllegalStateException(
                                "Customer has reached maximum number of active accounts (" +
                                MAX_ACTIVE_ACCOUNTS_PER_CUSTOMER + ")"));
                    }
                    log.debug("Active accounts validation passed: {}/{}", count, MAX_ACTIVE_ACCOUNTS_PER_CUSTOMER);
                    return Mono.empty();
                });
    }

    /**
     * Validate customer doesn't have duplicate account type
     */
    private Mono<Void> validateDuplicateAccountType(UUID customerId, String accountType) {
        return accountRepositoryPort.existsActiveAccountByCustomerIdAndAccountType(customerId, accountType)
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Customer {} already has active {} account", customerId, accountType);
                        return Mono.error(new IllegalStateException(
                                "Customer already has an active " + accountType + " account"));
                    }
                    log.debug("Duplicate account type validation passed");
                    return Mono.empty();
                });
    }

    // ========================================================================
    // PRIVATE: Event Publishing
    // ========================================================================

    /**
     * Publish AccountCreated event to Kafka
     * Fire-and-forget pattern (non-blocking)
     */
    private void publishAccountCreatedEvent(Account account) {
        try {
            AccountEventDTO event = AccountEventDTO.builder()
                    .eventType("account.created")
                    .accountNumber(account.getAccountNumber())
                    .customerId(account.getCustomerId())
                    .accountType(account.getAccountType().name())
                    .initialBalance(account.getInitialBalance())
                    .balance(account.getBalance())
                    .state(account.getState())
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisherPort.publish(
                    ACCOUNT_EVENTS_TOPIC,
                    account.getAccountNumber().toString(),
                    event
            ).subscribe(
                    v -> log.debug("ACCOUNT_CREATED event published: accountNumber={}", account.getAccountNumber()),
                    error -> log.error("Error publishing ACCOUNT_CREATED event: {}", error.getMessage())
            );
        } catch (Exception e) {
            log.error("Error publishing AccountCreated event: {}", e.getMessage());
        }
    }
}
