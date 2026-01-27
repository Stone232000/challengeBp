package com.espiedra.api.customer.service.application.ports.input;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Input Port (Use Case Interface) - Delete Customer
 * <p>
 * HEXAGONAL ARCHITECTURE - PRIMARY PORT:
 * This interface defines the contract for deleting customers.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Interface Segregation Principle (ISP): Focused on deletion operations only
 * - Single Responsibility Principle (SRP): Only handles customer deletion
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: PRIMARY PORT for commands
 * - Command Pattern: Represents delete command
 */
public interface DeleteCustomerUseCase {

    /**
     * Delete customer by ID (hard delete - removes from database).
     * Also deletes the associated Person record.
     * <p>
     * Business Rules:
     * - Customer must exist
     * - Hard delete: removes customer and person records from database
     * - Publishes DELETE event to Kafka
     * <p>
     *
     * @param customerId Customer UUID
     * @param correlationId Correlation ID for distributed tracing
     * @return Mono<Void> when deletion completes
     */
    Mono<Void> deleteCustomer(UUID customerId, String correlationId);
}
