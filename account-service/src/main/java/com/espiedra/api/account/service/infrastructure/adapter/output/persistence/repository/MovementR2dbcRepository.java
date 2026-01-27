package com.espiedra.api.account.service.infrastructure.adapter.output.persistence.repository;

import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.entity.MovementEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC Repository for MovementEntity using Spring Data R2DBC.
 */
@Repository
public interface MovementR2dbcRepository extends ReactiveCrudRepository<MovementEntity, UUID> {

    /**
     * Finds a movement by its unique movement ID.
     *
     * @param movementId the movement's unique identifier (UUID)
     * @return a Mono containing the movement if found, empty otherwise
     */
    Mono<MovementEntity> findByMovementId(UUID movementId);

    /**
     * Finds all movements for a specific account.
     *
     * @param accountNumber the account number
     * @return a Flux containing all movements for the account
     */
    Flux<MovementEntity> findByAccountNumber(Long accountNumber);

    /**
     * Finds movements for an account within a specific date range.
     * Results are ordered by creation date in descending order (newest first).
     *
     * <p>This method is typically used for generating account statements
     * and transaction history reports.</p>
     *
     * @param accountNumber the account number
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a Flux containing movements within the date range, ordered by date descending
     */
    @Query("SELECT * FROM movement WHERE account_number = :accountNumber AND created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    Flux<MovementEntity> findByAccountNumberAndDateRange(Long accountNumber, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Checks if a movement exists with the given transaction ID.
     * Used for idempotency checks to prevent duplicate transactions.
     *
     * @param transactionId the transaction's unique identifier (String)
     * @return a Mono containing true if exists, false otherwise
     */
    Mono<Boolean> existsByTransactionId(String transactionId);

    /**
     * Finds a movement by its transaction ID.
     * Useful for retrieving transaction details and ensuring idempotency.
     *
     * @param transactionId the transaction's unique identifier (String)
     * @return a Mono containing the movement if found, empty otherwise
     */
    Mono<MovementEntity> findByTransactionId(String transactionId);

    /**
     * Finds a movement by its idempotency key.
     * Used to prevent duplicate transactions in distributed systems.
     * Ensures exactly-once semantics even with network retries.
     *
     * @param idempotencyKey the idempotency key (UUID)
     * @return a Mono containing the movement if found, empty otherwise
     */
    Mono<MovementEntity> findByIdempotencyKey(UUID idempotencyKey);

    /**
     * Deletes all movements for a specific account.
     * Typically used when closing or deleting an account.
     *
     * @param accountNumber the account number
     * @return a Mono that completes when deletion is done
     */
    Mono<Void> deleteByAccountNumber(Long accountNumber);
}
