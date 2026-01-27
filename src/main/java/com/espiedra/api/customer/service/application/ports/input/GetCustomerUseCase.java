package com.espiedra.api.customer.service.application.ports.input;

import com.espiedra.api.customer.service.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Input Port (Use Case Interface) - Get Customer(s)
 * <p>
 * HEXAGONAL ARCHITECTURE - PRIMARY PORT:
 * This interface defines the contract for retrieving customers.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Interface Segregation Principle (ISP): Focused on retrieval operations only
 * - Single Responsibility Principle (SRP): Only handles customer queries
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: PRIMARY PORT for queries
 * - Query Pattern: Read-only operations
 */
public interface GetCustomerUseCase {

    /**
     * Get customer by ID.
     *
     * @param customerId Customer UUID
     * @return Mono with customer or empty if not found
     */
    Mono<Customer> getCustomerById(UUID customerId);

    /**
     * Get customer by identification number.
     *
     * @param identification Customer identification (cedula/passport)
     * @return Mono with customer or empty if not found
     */
    Mono<Customer> getCustomerByIdentification(String identification);

    /**
     * Get all customers with pagination.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Flux of customers
     */
    Flux<Customer> getAllCustomers(int page, int size);

    /**
     * Count all active customers.
     *
     * @return Mono of count customers
     */
    Mono<Long> count();

}
