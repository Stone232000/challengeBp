package com.espiedra.api.account.service.application.port.output;

import com.espiedra.api.account.service.domain.model.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * APPLICATION PORT (OUTPUT): MovementRepository
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining infrastructure capability
 * - Repository Pattern: Abstract data access layer
 * - Dependency Inversion Principle (DIP): Domain defines contract, infrastructure implements
 * <p>
 * SOLID PRINCIPLES:
 * - Dependency Inversion: High-level domain doesn't depend on low-level infrastructure
 * - Interface Segregation: Specific interface for movement persistence
 * - Single Responsibility: Only defines movement data access contract
 * <p>
 * IMPLEMENTATION:
 * - Implemented by MovementR2dbcAdapter in infrastructure layer
 * - Uses R2DBC for reactive database access
 * <p>
 * IMMUTABILITY NOTE:
 * - Movements are immutable (no update operations)
 * - Only INSERT and SELECT operations
 * <p>
 */
public interface MovementRepositoryPort {

    /**
     * Save a new movement (INSERT only, no updates)
     *
     * @param movement Movement to save
     * @return Mono<Movement> Saved movement
     */
    Mono<Movement> save(Movement movement);

    /**
     * Find a movement by its ID
     *
     * @param movementId Movement ID (UUID)
     * @return Mono<Movement> Movement if found, empty Mono otherwise
     */
    Mono<Movement> findByMovementId(UUID movementId);

    /**
     * Find all movements for an account
     *
     * @param accountNumber Account number
     * @return Flux<Movement> Stream of movements
     */
    Flux<Movement> findByAccountNumber(Long accountNumber);

    /**
     * Find all movements for an account in a date range
     *
     * @param accountNumber Account number
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Flux<Movement> Stream of movements
     */
    Flux<Movement> findByAccountNumberAndDateRange(Long accountNumber, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Check if a transaction ID already exists (for idempotency)
     *
     * @param transactionId Transaction ID (String)
     * @return Mono<Boolean> true if exists, false otherwise
     */
    Mono<Boolean> existsByTransactionId(String transactionId);

    /**
     * Find a movement by transaction ID
     *
     * @param transactionId Transaction ID (String)
     * @return Mono<Movement> Movement if found, empty Mono otherwise
     */
    Mono<Movement> findByTransactionId(String transactionId);

    /**
     * Find a movement by idempotency key
     * Used to prevent duplicate transactions in distributed systems
     *
     * @param idempotencyKey Idempotency key (UUID)
     * @return Mono<Movement> Movement if found, empty Mono otherwise
     */
    Mono<Movement> findByIdempotencyKey(UUID idempotencyKey);

    /**
     * Delete all movements for an account
     * Used when cascading account deletion
     *
     * @param accountNumber Account number
     * @return Mono<Void> Completion signal
     */
    Mono<Void> deleteByAccountNumber(Long accountNumber);
}
