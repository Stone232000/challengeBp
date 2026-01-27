package com.espiedra.api.customer.service.application.ports.input;

import com.espiedra.api.customer.service.application.dto.CustomerCreationResult;
import com.espiedra.api.customer.service.domain.model.Customer;
import reactor.core.publisher.Mono;

/**
 * Input Port (Use Case Interface) - Create Customer
 * <p>
 * HEXAGONAL ARCHITECTURE - PRIMARY PORT:
 * This interface defines the contract for creating a customer.
 * It is independent of any framework or infrastructure.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Interface Segregation Principle (ISP): Single, focused interface
 * - Dependency Inversion Principle (DIP): High-level policy (domain) doesn't depend on low-level details
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: This is a PRIMARY PORT (input) in hexagonal architecture
 * - Command Pattern: Represents a command to create a customer
 */
public interface CreateCustomerUseCase {

    /**
     * Create a new customer in the system.
     * <p>
     * Business Rules:
     * - Customer identification must be unique
     * - Password must be encrypted with BCrypt
     * - Customer is created in ACTIVE state by default
     * - Returns JWT token for immediate authentication
     *
     * @param customer Customer domain model to create
     * @param rawPassword Plain text password (will be encrypted)
     * @param correlationId Correlation ID for distributed tracing (from HTTP X-Correlation-Id header)
     * @return Mono with created customer and JWT token
     */
    Mono<CustomerCreationResult> createCustomer(Customer customer, String rawPassword, String correlationId);

}
