package com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.repository;

import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity.CustomerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * R2DBC Reactive Repository - Customer
 * <p>
 * Spring Data R2DBC repository for Customer entity.
 * Provides reactive CRUD operations with optimistic locking support.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is part of the INFRASTRUCTURE layer (Secondary Adapter).
 * The adapter will use this repository and implement the CustomerRepositoryPort.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Repository Pattern: Data access abstraction
 * - Reactive Pattern: Non-blocking I/O with Reactor
 * - Optimistic Locking: Automatic version management by Spring Data R2DBC
 */
@Repository
public interface CustomerR2dbcRepository extends ReactiveCrudRepository<CustomerEntity, UUID> {

    /**
     * Find all customers with pagination support.
     *
     * @param pageable Pagination information
     * @return Flux of CustomerEntity
     */
    Flux<CustomerEntity> findAllBy(Pageable pageable);

    /**
     * Find all active customers.
     *
     * @return Flux of CustomerEntity
     */
    Flux<CustomerEntity> findAllByStateTrue();

    /**
     * Count active customers.
     *
     * @return Mono of Long
     */
    Mono<Long> countByStateTrue();
}
