package com.espiedra.api.customer.service.application.ports.input;

import com.espiedra.api.customer.service.domain.model.Customer;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Input Port (Use Case Interface) - Update Customer
 * <p>
 * HEXAGONAL ARCHITECTURE - PRIMARY PORT:
 * This interface defines the contract for updating customers.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Interface Segregation Principle (ISP): Focused on update operations only
 * - Single Responsibility Principle (SRP): Only handles customer updates
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: PRIMARY PORT for commands
 * - Command Pattern: Represents update commands
 */
public interface UpdateCustomerUseCase {

    /**
     * Update customer information.
     * <p>
     * Business Rules:
     * - Customer must exist
     * - Version check for optimistic locking
     * - Publishes UPDATE event to Kafka
     *
     * @param customerId Customer UUID
     * @param updatedCustomer Updated customer data
     * @param expectedVersion Expected version for optimistic locking
     * @param correlationId Correlation ID for distributed tracing
     * @return Mono with updated customer
     */
    Mono<Customer> updateCustomer(UUID customerId, Customer updatedCustomer, Long expectedVersion, String correlationId);

    /**
     * Update customer password.
     *
     * @param customerId Customer UUID
     * @param currentPassword Current password (for validation)
     * @param newPassword New password (will be encrypted)
     * @param expectedVersion Expected version for optimistic locking
     * @param correlationId Correlation ID for distributed tracing
     * @return Mono with updated customer
     */
    Mono<Customer> updatePassword(UUID customerId, String currentPassword, String newPassword, Long expectedVersion, String correlationId);

    /**
     * Activate customer account.
     *
     * @param customerId Customer UUID
     * @param expectedVersion Expected version for optimistic locking
     * @param correlationId Correlation ID for distributed tracing
     * @return Mono with activated customer
     */
    Mono<Customer> activateCustomer(UUID customerId, Long expectedVersion, String correlationId);

    /**
     * Deactivate customer account.
     *
     * @param customerId Customer UUID
     * @param expectedVersion Expected version for optimistic locking
     * @param correlationId Correlation ID for distributed tracing
     * @return Mono with deactivated customer
     */
    Mono<Customer> deactivateCustomer(UUID customerId, Long expectedVersion, String correlationId);
}
