package com.espiedra.api.account.service.application.port.input;

import com.espiedra.api.account.service.domain.model.Account;
import reactor.core.publisher.Mono;

/**
 * APPLICATION PORT (INPUT): UpdateAccountUseCase
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining business capability
 * - Command Pattern: Represents a command to update an account
 * - Interface Segregation Principle (ISP): Specific interface for one use case
 * <p>
 * SOLID PRINCIPLES:
 * - Interface Segregation: Clients depend only on update operation
 * - Dependency Inversion: Domain depends on abstraction, not concrete implementation
 * - Single Responsibility: Only defines account update contract
 * <p>
 * IMPLEMENTATION:
 * - Implemented by AccountService in application layer
 * - Called by REST controllers (primary adapters)
 * <p>
 * BUSINESS RULES:
 * - Account must exist
 * - Cannot update balance directly (only via movements)
 * - Can update: accountType, state
 * - Uses optimistic locking (version) to prevent concurrent updates
 * <p>
 * NOTE:
 * - For balance updates, use CreateMovementUseCase instead
 * - This is for administrative updates (e.g., change account type, activate/deactivate)
 * <p>
 */
public interface UpdateAccountUseCase {

    /**
     * Update an existing account
     * <p>
     * BUSINESS FLOW:
     * 1. Validate account exists
     * 2. Update allowed fields (accountType, state)
     * 3. Optimistic locking check (version)
     * 4. Save updated account
     * 5. Publish ACCOUNT_UPDATED event to Kafka
     * 6. Return updated account
     *
     * @param accountNumber Account number to update
     * @param account Updated account data
     * @return Mono<Account> Updated account
     * <p>
     */
    Mono<Account> updateAccount(Long accountNumber, Account account);
}
