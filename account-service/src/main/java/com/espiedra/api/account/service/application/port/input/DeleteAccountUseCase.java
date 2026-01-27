package com.espiedra.api.account.service.application.port.input;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * APPLICATION PORT (INPUT): DeleteAccountUseCase
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining business capability
 * - Command Pattern: Represents a command to delete an account
 * - Interface Segregation Principle (ISP): Specific interface for one use case
 * <p>
 * SOLID PRINCIPLES:
 * - Interface Segregation: Clients depend only on delete operation
 * - Dependency Inversion: Domain depends on abstraction, not concrete implementation
 * - Single Responsibility: Only defines account deletion contract
 * <p>
 * IMPLEMENTATION:
 * - Implemented by AccountService in application layer
 * - Called by REST controllers (primary adapters)
 * <p>
 * DELETE STRATEGIES:
 * - SOFT DELETE (PATCH /accounts/{id} with state=false): Account.state = false
 * - HARD DELETE (DELETE /accounts/{id}): Permanently remove account
 * <p>
 * BUSINESS RULES:
 * - Account must exist
 * - Cascade delete movements if hard delete
 * - Publish account.deleted. event to Kafka
 *
 */
public interface DeleteAccountUseCase {

    /**
     * Delete an account
     * <p>
     * BUSINESS FLOW:
     * 1. Validate account exists
     * 2. Delete associated movements (cascade)
     * 3. Delete account record
     * 4. Publish ACCOUNT_DELETED event to Kafka
     * 5. Return void
     *
     * @param accountNumber Account number to delete
     * @return Mono<Void> Completion signal
     *
     */
    Mono<Void> deleteAccount(Long accountNumber);

    /**
     * Delete all accounts for a customer
     * <p>
     * BUSINESS FLOW:
     * 1. Find all accounts for the customer
     * 2. For each account:
     *    - Delete associated movements (cascade)
     *    - Delete account record
     *    - Publish ACCOUNT_DELETED event to Kafka
     * 3. Return count of deleted accounts
     * <p>
     * USE CASE:
     * - Called from Kafka consumer when customer.deleted. event is received
     * - Maintains referential integrity across microservices
     *
     * @param customerId Customer ID (UUID)
     * @return Mono<Long> Number of accounts deleted
     */
    Mono<Long> deleteAccountsByCustomerId(UUID customerId);
}
