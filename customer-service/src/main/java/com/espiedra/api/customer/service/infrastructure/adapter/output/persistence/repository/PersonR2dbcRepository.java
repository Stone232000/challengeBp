package com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.repository;

import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity.PersonEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * R2DBC Reactive Repository - Person
 * <p>
 * Spring Data R2DBC repository for Person entity.
 * Provides reactive CRUD operations.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is part of the INFRASTRUCTURE layer (Secondary Adapter).
 * The adapter will use this repository and implement the CustomerRepositoryPort.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Repository Pattern: Data access abstraction
 * - Reactive Pattern: Non-blocking I/O with Reactor
 */
@Repository
public interface PersonR2dbcRepository extends ReactiveCrudRepository<PersonEntity, UUID> {

    /**
     * Find person by identification number.
     *
     * @param identification Customer identification
     * @return Mono of PersonEntity
     */
    Mono<PersonEntity> findByIdentification(String identification);

    /**
     * Check if person exists by identification.
     *
     * @param identification Customer identification
     * @return Mono of Boolean
     */
    Mono<Boolean> existsByIdentification(String identification);
}
