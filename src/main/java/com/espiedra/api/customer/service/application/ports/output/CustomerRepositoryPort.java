package com.espiedra.api.customer.service.application.ports.output;

import com.espiedra.api.customer.service.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Output Port (Repository Interface) - Customer Repository
 * <p>
 * HEXAGONAL ARCHITECTURE - SECONDARY PORT:
 * This interface defines the contract for customer persistence operations.
 * It is independent of any framework or infrastructure (R2DBC, JPA, etc.).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Domain depends on abstraction, not implementation
 * - Interface Segregation Principle (ISP): Single, focused repository interface
 * - Single Responsibility Principle (SRP): Only handles customer persistence
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: This is a SECONDARY PORT (output) in hexagonal architecture
 * - Repository Pattern: Abstracts data access layer
 */
public interface CustomerRepositoryPort {

    /**
     * Save a new customer.
     *
     * @param customer Customer to save
     * @return Mono with saved customer
     */
    Mono<Customer> save(Customer customer);

    /**
     * Update an existing customer.
     *
     * @param customer Customer to update
     * @return Mono with updated customer
     */
    Mono<Customer> update(Customer customer);

    /**
     * Find customer by ID.
     *
     * @param customerId Customer UUID
     * @return Mono with customer or empty if not found
     */
    Mono<Customer> findById(UUID customerId);

    /**
     * Find customer by identification number.
     *
     * @param identification Customer identification
     * @return Mono with customer or empty if not found
     */
    Mono<Customer> findByIdentification(String identification);

    /**
     * Find all customers with pagination.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Flux of customers
     */
    Flux<Customer> findAll(int page, int size);

    /**
     * Check if customer exists by identification.
     *
     * @param identification Customer identification
     * @return Mono with true if exists, false otherwise
     */
    Mono<Boolean> existsByIdentification(String identification);

    /**
     * Delete customer by ID (hard delete - removes from database).
     * Also deletes the associated Person record.
     * <p>
     *
     * @param customerId Customer UUID
     * @return Mono<Void> when deletion completes
     */
    Mono<Void> deleteById(UUID customerId);

    /**
     * Count total customers.
     *
     * @return Mono with count
     */
    Mono<Long> count();

}
