package com.espiedra.api.account.service.infrastructure.adapter.output.persistence.repository;

import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.entity.AccountEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * R2DBC Repository for AccountEntity using Spring Data R2DBC.
 */
@Repository
public interface AccountR2dbcRepository extends ReactiveCrudRepository<AccountEntity, Long> {

    /**
     * Finds an account by its account number.
     *
     * @param accountNumber the unique account number
     * @return a Mono containing the account if found, empty otherwise
     */
    Mono<AccountEntity> findByAccountNumber(Long accountNumber);

    /**
     * Finds all accounts belonging to a specific customer.
     *
     * @param customerId the customer's unique identifier
     * @return a Flux containing all accounts for the customer
     */
    Flux<AccountEntity> findByCustomerId(UUID customerId);

    /**
     * Checks if an account exists with the given account number.
     *
     * @param accountNumber the account number to check
     * @return a Mono containing true if exists, false otherwise
     */
    Mono<Boolean> existsByAccountNumber(Long accountNumber);

    /**
     * Counts the number of active accounts for a specific customer.
     * Only accounts with state = true are counted.
     *
     * @param customerId the customer's unique identifier
     * @return a Mono containing the count of active accounts
     */
    @Query("SELECT COUNT(*) FROM account WHERE customer_id = :customerId AND state = true")
    Mono<Long> countActiveAccountsByCustomerId(UUID customerId);

    /**
     * Checks if a customer already has an active account of a specific type.
     * <p>
     * BUSINESS RULE: A customer can only have ONE active account per type
     * - ONE AHORRO (savings) account
     * - ONE CORRIENTE (checking) account
     *
     * @param customerId the customer's unique identifier
     * @param accountType the account type (AHORRO or CORRIENTE)
     * @return a Mono containing true if an active account of this type exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM account WHERE customer_id = :customerId AND account_type = :accountType AND state = true)")
    Mono<Boolean> existsActiveAccountByCustomerIdAndAccountType(UUID customerId, String accountType);

    /**
     * Deletes an account by its account number.
     *
     * @param accountNumber the account number to delete
     * @return a Mono that completes when deletion is done
     */
    Mono<Void> deleteByAccountNumber(Long accountNumber);
}
