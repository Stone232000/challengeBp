package com.espiedra.api.account.service.infrastructure.adapter.output.persistence;

import com.espiedra.api.account.service.application.port.output.MovementRepositoryPort;
import com.espiedra.api.account.service.domain.model.Movement;
import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.mapper.MovementEntityMapper;
import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.repository.MovementR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistence Adapter for Movement (Transaction) entity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovementPersistenceAdapterPort implements MovementRepositoryPort {

    private final MovementR2dbcRepository movementR2dbcRepository;
    private final MovementEntityMapper movementEntityMapper;

    /**
     * Saves a movement to the database.
     *
     * @param movement the movement to save
     * @return a Mono containing the saved movement
     */
    @Override
    public Mono<Movement> save(Movement movement) {
        log.debug("Saving movement: {}", movement.getMovementId());
        return Mono.just(movement)
            .map(movementEntityMapper::toEntity)
            .flatMap(movementR2dbcRepository::save)
            .map(movementEntityMapper::toDomain)
            .doOnSuccess(saved -> log.info("Movement saved successfully: {}", saved.getMovementId()))
            .doOnError(error -> log.error("Error saving movement: {}", error.getMessage()));
    }

    /**
     * Finds a movement by its movement ID.
     *
     * @param movementId the movement's unique identifier (UUID)
     * @return a Mono containing the movement if found, empty otherwise
     */
    @Override
    public Mono<Movement> findByMovementId(UUID movementId) {
        log.debug("Finding movement by ID: {}", movementId);
        return movementR2dbcRepository.findByMovementId(movementId)
            .map(movementEntityMapper::toDomain);
    }

    /**
     * Finds all movements for a specific account.
     *
     * @param accountNumber the account number
     * @return a Flux containing all movements for the account
     */
    @Override
    public Flux<Movement> findByAccountNumber(Long accountNumber) {
        log.debug("Finding movements by account number: {}", accountNumber);
        return movementR2dbcRepository.findByAccountNumber(accountNumber)
            .map(movementEntityMapper::toDomain);
    }

    /**
     * Finds movements for an account within a date range.
     *
     * @param accountNumber the account number
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return a Flux containing movements within the date range
     */
    @Override
    public Flux<Movement> findByAccountNumberAndDateRange(Long accountNumber, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Finding movements by account number: {} and date range: {} to {}", accountNumber, startDate, endDate);
        return movementR2dbcRepository.findByAccountNumberAndDateRange(accountNumber, startDate, endDate)
            .map(movementEntityMapper::toDomain);
    }

    /**
     * Checks if a movement exists with the given transaction ID.
     *
     * @param transactionId the transaction's unique identifier (String)
     * @return a Mono containing true if exists, false otherwise
     */
    @Override
    public Mono<Boolean> existsByTransactionId(String transactionId) {
        log.debug("Checking if movement exists by transaction ID: {}", transactionId);
        return movementR2dbcRepository.existsByTransactionId(transactionId);
    }

    /**
     * Finds a movement by its transaction ID.
     *
     * @param transactionId the transaction's unique identifier (String)
     * @return a Mono containing the movement if found, empty otherwise
     */
    @Override
    public Mono<Movement> findByTransactionId(String transactionId) {
        log.debug("Finding movement by transaction ID: {}", transactionId);
        return movementR2dbcRepository.findByTransactionId(transactionId)
            .map(movementEntityMapper::toDomain);
    }

    /**
     * Finds a movement by its idempotency key.
     * Used to prevent duplicate transactions in distributed systems.
     *
     * @param idempotencyKey the idempotency key (UUID)
     * @return a Mono containing the movement if found, empty otherwise
     */
    @Override
    public Mono<Movement> findByIdempotencyKey(UUID idempotencyKey) {
        log.debug("Finding movement by idempotency key: {}", idempotencyKey);
        return movementR2dbcRepository.findByIdempotencyKey(idempotencyKey)
            .map(movementEntityMapper::toDomain);
    }

    /**
     * Deletes all movements for a specific account.
     *
     * @param accountNumber the account number
     * @return a Mono that completes when deletion is done
     */
    @Override
    public Mono<Void> deleteByAccountNumber(Long accountNumber) {
        log.debug("Deleting movements for account: {}", accountNumber);
        return movementR2dbcRepository.deleteByAccountNumber(accountNumber)
            .doOnSuccess(v -> log.info("Movements deleted successfully for account: {}", accountNumber));
    }
}
