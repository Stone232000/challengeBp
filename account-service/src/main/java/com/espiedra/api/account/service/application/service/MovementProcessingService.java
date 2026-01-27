package com.espiedra.api.account.service.application.service;

import com.espiedra.api.account.service.application.dto.MovementEventDTO;
import com.espiedra.api.account.service.application.port.input.CreateMovementUseCase;
import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.application.port.output.EventPublisherPort;
import com.espiedra.api.account.service.application.port.output.MovementRepositoryPort;
import com.espiedra.api.account.service.domain.exception.AccountNotActiveException;
import com.espiedra.api.account.service.domain.exception.AccountNotFoundException;
import com.espiedra.api.account.service.domain.exception.DuplicateTransactionException;
import com.espiedra.api.account.service.domain.exception.InsufficientBalanceException;
import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.domain.model.Movement;
import com.espiedra.api.account.service.domain.model.MovementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SERVICE: Movement Processing Service
 * <p>
 * RESPONSIBILITY: Process financial movements (debits, credits, reversals)
 * <p>
 * DESIGN PATTERNS:
 * - Service Pattern: Encapsulates movement processing logic
 * - Single Responsibility: Only handles movement creation and validation
 * <p>
 * BUSINESS RULES:
 * - Transaction ID must be unique (idempotency)
 * - Idempotency key prevents duplicate requests
 * - Amount must be > 0
 * - Account must exist and be active
 * - Sufficient balance for debits (with overdraft limit)
 * <p>
 * CLEAN CODE REFACTORING:
 * Extracted from AccountService.java (1023 lines) to improve:
 * - Single Responsibility Principle
 * - Testability
 * - Maintainability
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MovementProcessingService implements CreateMovementUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final MovementRepositoryPort movementRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    private static final String MOVEMENT_EVENTS_TOPIC = "banking.movement.events";
    private static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("10000.00");

    /**
     * Create a new movement (deposit, withdrawal, or reversal)
     * <p>
     * BUSINESS FLOW:
     * 1. Validate transaction ID is unique
     * 2. Validate idempotency key (if provided)
     * 3. Validate amount is positive
     * 4. Validate account exists and is active
     * 5. Process movement and update balance
     * 6. Publish MovementCreated event to Kafka
     *
     * @param movement Movement to create
     * @return Mono<Movement> Created movement with updated balances
     */
    @Override
    public Mono<Movement> createMovement(Movement movement) {
        log.info("Creating movement: account={}, type={}, amount={}, txId={}, idempotencyKey={}",
                movement.getAccountNumber(), movement.getMovementType(),
                movement.getAmount(), movement.getTransactionId(), movement.getIdempotencyKey());

        return validateMovementIdempotency(movement.getTransactionId())
                .then(validateIdempotencyKey(movement.getIdempotencyKey()))
                .then(validateMovementAmount(movement))
                .then(accountRepositoryPort.findByAccountNumber(movement.getAccountNumber()))
                .switchIfEmpty(Mono.error(new AccountNotFoundException(movement.getAccountNumber())))
                .flatMap(this::validateAccountIsActive)
                .flatMap(account -> processMovement(account, movement))
                .doOnSuccess(savedMovement -> {
                    log.info("Movement created successfully: movementId={}, accountNumber={}, balanceAfter={}",
                            savedMovement.getMovementId(), savedMovement.getAccountNumber(),
                            savedMovement.getBalanceAfter());
                    publishMovementCreatedEvent(savedMovement);
                })
                .doOnError(error -> log.error("Error creating movement for account {}: {}",
                        movement.getAccountNumber(), error.getMessage()));
    }

    // ========================================================================
    // PRIVATE: Validation Methods
    // ========================================================================

    /**
     * Validate transaction ID is unique (idempotency check)
     */
    private Mono<Void> validateMovementIdempotency(String transactionId) {
        return movementRepositoryPort.existsByTransactionId(transactionId)
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Duplicate transaction ID detected: {}", transactionId);
                        return Mono.error(new DuplicateTransactionException(transactionId));
                    }
                    log.debug("Transaction ID {} is unique, validation passed", transactionId);
                    return Mono.empty();
                });
    }

    /**
     * Validate idempotency key is unique (prevents duplicate requests)
     */
    private Mono<Void> validateIdempotencyKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            log.debug("No idempotency key provided, skipping validation");
            return Mono.empty();
        }

        return movementRepositoryPort.findByIdempotencyKey(idempotencyKey)
                .flatMap(existingMovement -> {
                    log.warn("Duplicate idempotency key detected: {} (existing movementId: {})",
                            idempotencyKey, existingMovement.getMovementId());
                    return Mono.error(new com.espiedra.api.account.service.domain.exception.DuplicateIdempotencyKeyException(
                            idempotencyKey, existingMovement.getMovementId()));
                })
                .then()
                .doOnSuccess(v -> log.debug("Idempotency key {} is unique, validation passed", idempotencyKey));
    }

    /**
     * Validate movement amount is positive
     */
    private Mono<Void> validateMovementAmount(Movement movement) {
        if (movement.getAmount() == null || movement.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid movement amount: {}", movement.getAmount());
            return Mono.error(new IllegalArgumentException("Movement amount must be greater than 0"));
        }
        return Mono.empty();
    }

    /**
     * Validate account is active
     */
    private Mono<Account> validateAccountIsActive(Account account) {
        if (!account.isActive()) {
            log.warn("Account {} is not active, cannot create movement", account.getAccountNumber());
            return Mono.error(new AccountNotActiveException(account.getAccountNumber()));
        }
        log.debug("Account {} is active, validation passed", account.getAccountNumber());
        return Mono.just(account);
    }

    // ========================================================================
    // PRIVATE: Movement Processing
    // ========================================================================

    /**
     * Process movement: update balance and save
     */
    private Mono<Movement> processMovement(Account account, Movement movement) {
        BigDecimal balanceBefore = account.getBalance();
        log.debug("Processing movement: balanceBefore={}, type={}, amount={}",
                balanceBefore, movement.getMovementType(), movement.getAmount());

        try {
            BigDecimal balanceAfter;

            if (movement.getMovementType() == MovementType.CREDITO) {
                balanceAfter = account.credit(movement.getAmount());
                log.debug("Credit processed: balanceAfter={}", balanceAfter);

            } else if (movement.getMovementType() == MovementType.DEBITO) {
                balanceAfter = account.debit(movement.getAmount());
                log.debug("Debit processed: balanceAfter={}", balanceAfter);

            } else if (movement.getMovementType() == MovementType.REVERSA) {
                balanceAfter = balanceBefore;
                log.debug("Reversal movement - trigger will handle balance update: reversedMovementId={}",
                        movement.getReversedMovementId());

            } else {
                return Mono.error(new IllegalArgumentException("Invalid movement type: " + movement.getMovementType()));
            }

            movement.setBalanceBefore(balanceBefore);
            movement.setBalanceAfter(balanceAfter);

            return movementRepositoryPort.save(movement)
                    .doOnSuccess(savedMovement -> log.debug("Movement saved: balanceBefore={}, balanceAfter={}",
                            savedMovement.getBalanceBefore(), savedMovement.getBalanceAfter()));

        } catch (IllegalStateException e) {
            log.warn("Insufficient balance for withdrawal: account={}, balance={}, requestedAmount={}",
                    account.getAccountNumber(), balanceBefore, movement.getAmount());
            return Mono.error(new InsufficientBalanceException(balanceBefore, movement.getAmount(), OVERDRAFT_LIMIT));
        } catch (IllegalArgumentException e) {
            log.error("Invalid movement: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    // ========================================================================
    // PRIVATE: Event Publishing
    // ========================================================================

    /**
     * Publish MovementCreated event to Kafka
     */
    private void publishMovementCreatedEvent(Movement movement) {
        try {
            MovementEventDTO event = MovementEventDTO.builder()
                    .eventType("movement.created")
                    .movementId(movement.getMovementId())
                    .accountNumber(movement.getAccountNumber())
                    .movementType(movement.getMovementType().name())
                    .amount(movement.getAmount())
                    .balanceBefore(movement.getBalanceBefore())
                    .balanceAfter(movement.getBalanceAfter())
                    .transactionId(movement.getTransactionId())
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisherPort.publish(
                    MOVEMENT_EVENTS_TOPIC,
                    movement.getMovementId().toString(),
                    event
            ).subscribe(
                    v -> log.debug("MOVEMENT_CREATED event published: movementId={}", movement.getMovementId()),
                    error -> log.error("Error publishing MOVEMENT_CREATED event: {}", error.getMessage())
            );
        } catch (Exception e) {
            log.error("Error publishing MovementCreated event: {}", e.getMessage());
        }
    }
}
