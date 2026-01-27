package com.espiedra.api.account.service.infrastructure.adapter.output.persistence;

import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.mapper.AccountEntityMapper;
import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.repository.AccountR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Persistence Adapter for Account entity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapterPort implements AccountRepositoryPort {

    private final AccountR2dbcRepository accountR2dbcRepository;
    private final AccountEntityMapper accountEntityMapper;

    /**
     * Saves an account to the database.
     * <p>
     * Spring Data R2DBC automatically determines INSERT vs UPDATE:
     * - If accountNumber is null → INSERT (database generates ID via sequence)
     * - If accountNumber is not null → UPDATE
     * <p>
     * The database sequence 'account_number_seq' generates the account_number
     * automatically when inserting new accounts.
     * <p>
     * @param account the account to save
     * @return a Mono containing the saved account with generated ID
     */
    @Override
    public Mono<Account> save(Account account) {
        log.debug("Saving account: {}", account.getAccountNumber());
        return Mono.just(account)
            .map(accountEntityMapper::toEntity)
            .flatMap(accountR2dbcRepository::save)
            .map(accountEntityMapper::toDomain)
            .doOnSuccess(saved -> log.info("Account saved successfully: {}", saved.getAccountNumber()))
            .doOnError(error -> log.error("Error saving account: {}", error.getMessage()));
    }

    /**
     * Finds an account by its account number.
     *
     * @param accountNumber the unique account number
     * @return a Mono containing the account if found, empty otherwise
     */
    @Override
    public Mono<Account> findByAccountNumber(Long accountNumber) {
        log.debug("Finding account by number: {}", accountNumber);
        return accountR2dbcRepository.findByAccountNumber(accountNumber)
            .map(accountEntityMapper::toDomain);
    }

    /**
     * Finds all accounts for a specific customer.
     *
     * @param customerId the customer's unique identifier
     * @return a Flux containing all customer accounts
     */
    @Override
    public Flux<Account> findByCustomerId(UUID customerId) {
        log.debug("Finding accounts by customer ID: {}", customerId);
        return accountR2dbcRepository.findByCustomerId(customerId)
            .map(accountEntityMapper::toDomain);
    }

    /**
     * Finds all accounts in the system.
     *
     * @return a Flux containing all accounts
     */
    @Override
    public Flux<Account> findAll() {
        log.debug("Finding all accounts");
        return accountR2dbcRepository.findAll()
            .map(accountEntityMapper::toDomain)
            .doOnComplete(() -> log.debug("Finished loading all accounts"));
    }

    /**
     * Deletes an account by its account number.
     *
     * @param accountNumber the account number to delete
     * @return a Mono that completes when deletion is done
     */
    @Override
    public Mono<Void> deleteByAccountNumber(Long accountNumber) {
        log.debug("Deleting account: {}", accountNumber);
        return accountR2dbcRepository.deleteByAccountNumber(accountNumber)
            .doOnSuccess(v -> log.info("Account deleted successfully: {}", accountNumber));
    }


    /**
     * Counts active accounts for a specific customer.
     *
     * @param customerId the customer's unique identifier
     * @return a Mono containing the count of active accounts
     */
    @Override
    public Mono<Long> countActiveAccountsByCustomerId(UUID customerId) {
        return accountR2dbcRepository.countActiveAccountsByCustomerId(customerId);
    }

    /**
     * Checks if a customer already has an active account of a specific type.
     * <p>
     * BUSINESS RULE: A customer can only have ONE active account per type
     *
     * @param customerId the customer's unique identifier
     * @param accountType the account type (AHORRO or CORRIENTE)
     * @return a Mono containing true if exists, false otherwise
     */
    @Override
    public Mono<Boolean> existsActiveAccountByCustomerIdAndAccountType(UUID customerId, String accountType) {
        log.debug("Checking if customer {} has an active {} account", customerId, accountType);
        return accountR2dbcRepository.existsActiveAccountByCustomerIdAndAccountType(customerId, accountType);
    }
}
