package com.espiedra.api.account.service.application.port.output;

import com.espiedra.api.account.service.domain.model.Customer;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * APPLICATION PORT (OUTPUT): CustomerServiceClient
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining infrastructure capability
 * - Client Pattern: Abstract external service communication
 * - Circuit Breaker Pattern: Fault tolerance with fallback
 * - Dependency Inversion Principle (DIP): Domain defines contract, infrastructure implements
 * <p>
 * SOLID PRINCIPLES:
 * - Dependency Inversion: High-level domain doesn't depend on low-level WebClient
 * - Interface Segregation: Specific interface for customer service communication
 * - Single Responsibility: Only defines customer service client contract
 * <p>
 * IMPLEMENTATION:
 * - Implemented by CustomerWebClient in infrastructure layer
 * - Uses Spring WebClient for reactive HTTP calls
 * - Implements Circuit Breaker pattern for resilience
 * - Fallback to cache when service unavailable
 * <p>
 * RESILIENCE STRATEGY:
 * 1. Try WebClient (2 second timeout)
 * 2. If fails, fallback to cached customer data
 * 3. If cache stale, still use it (degraded mode)
 * <p>
 */
public interface CustomerServiceClientPort {

    /**
     * Validate that a customer exists and is active
     * <p>
     * CIRCUIT BREAKER PATTERN:
     * - Timeout: 2 seconds
     * - Retry: 1 attempt
     * - Fallback: Return customer from cache
     * <p>
     * @param customerId Customer ID to validate
     * @return Mono<Customer> Customer if valid and active
     * <p>
     */
    Mono<Customer> validateCustomer(UUID customerId);

    /**
     * Get customer details from Customer Service
     * Without fallback to cache (for fresh data requirements)
     *
     * @param customerId Customer ID
     * @return Mono<Customer> Customer details
     */
    Mono<Customer> getCustomer(UUID customerId);

    /**
     * Check if customer exists (existence check only)
     *
     * @param customerId Customer ID
     * @return Mono<Boolean> true if exists, false otherwise
     */
    Mono<Boolean> exists(UUID customerId);
}
