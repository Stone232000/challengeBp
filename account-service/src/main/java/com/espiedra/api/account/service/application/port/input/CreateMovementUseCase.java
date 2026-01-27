package com.espiedra.api.account.service.application.port.input;

import com.espiedra.api.account.service.domain.model.Movement;
import reactor.core.publisher.Mono;

/**
 * APPLICATION PORT (INPUT): CreateMovementUseCase
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining business capability
 * - Command Pattern: Represents a command to create a movement
 * - Interface Segregation Principle (ISP): Specific interface for one use case
 * - Idempotency Pattern: Uses transactionId to prevent duplicate transactions
 * <p>
 * SOLID PRINCIPLES:
 * - Interface Segregation: Clients depend only on create movement operation
 * - Dependency Inversion: Domain depends on abstraction, not concrete implementation
 * - Single Responsibility: Only defines movement creation contract
 * <p>
 * IMPLEMENTATION:
 * - Implemented by AccountService in application layer
 * - Called by REST controllers (primary adapters)
 * <p>
 * BUSINESS RULES ENFORCED:
 * - Account must exist and be active
 * - Amount must be positive (> 0)
 * - Balance must be sufficient
 * - Transaction ID must be unique (idempotency)
 * - Balance is updated atomically
 */
public interface CreateMovementUseCase {

    /**
     * Create a new transaction movement (deposit or withdrawal)
     * <p>
     * BUSINESS FLOW:
     * 1. Validate account exists and is active
     * 2. Check transaction ID is unique (idempotency)
     * 3. Validate amount > 0
     * 4. For DEBITO: Validate sufficient balance
     * 5. Update account balance (credit/debit)
     * 6. Create movement record with before/after balance
     * 7. Save both account and movement (transactional)
     * 8. Publish MOVEMENT_CREATED event to Kafka
     * 9. Return created movement
     *
     * @param movement Movement to create (accountNumber, movementType, amount, transactionId)
     * @return Mono<Movement> Created movement with balanceBefore and balanceAfter
     */
    Mono<Movement> createMovement(Movement movement);
}
