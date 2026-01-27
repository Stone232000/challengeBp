package com.espiedra.api.customer.service.infrastructure.adapter.output.persistence;

import com.espiedra.api.customer.service.application.ports.output.CustomerRepositoryPort;
import com.espiedra.api.customer.service.domain.model.Customer;
import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity.CustomerEntity;
import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity.PersonEntity;
import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.mapper.CustomerPersistenceMapper;
import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.repository.CustomerR2dbcRepository;
import com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.repository.PersonR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Customer Persistence Adapter - Infrastructure Layer
 * <p>
 * This adapter implements the CustomerRepositoryPort (output port) using R2DBC.
 * It coordinates operations between Person and Customer tables.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is a SECONDARY ADAPTER that implements an OUTPUT PORT.
 * It converts between domain models and database entities.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Implements port interface from application layer
 * - Single Responsibility Principle (SRP): Only handles persistence operations
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts R2DBC repositories to domain port interface
 * - Mapper Pattern: Uses MapStruct to convert entities ↔ domain models
 * - Transaction Coordination: Manages Person and Customer persistence together
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final PersonR2dbcRepository personRepository;
    private final CustomerR2dbcRepository customerRepository;
    private final CustomerPersistenceMapper mapper;

    @Override
    public Mono<Customer> save(Customer customer) {
        log.debug("Saving customer: {}", customer.getCustomerId());

        // Convert domain to entities
        // Customer extends Person, so we extract Person fields from Customer
        PersonEntity personEntity = mapper.customerToPersonEntity(customer);
        CustomerEntity customerEntity = mapper.customerToEntity(customer);

        // Save Person first, then Customer (sequential operations due to FK constraint)
        return personRepository.save(personEntity)
                .flatMap(savedPerson -> {
                    // Set the person_id foreign key in customer entity
                    customerEntity.setPersonId(savedPerson.getPersonId());
                    return customerRepository.save(customerEntity);
                })
                .flatMap(savedCustomer -> findById(savedCustomer.getCustomerId()))
                .doOnSuccess(saved -> log.debug("Customer saved successfully: {}", saved.getCustomerId()))
                .doOnError(error -> log.error("Error saving customer: {}", error.getMessage()));
    }

    @Override
    public Mono<Customer> update(Customer customer) {
        log.debug("Updating customer: {}", customer.getCustomerId());

        // Convert domain to entities
        // Customer extends Person, so we extract Person fields from Customer
        PersonEntity personEntity = mapper.customerToPersonEntity(customer);
        CustomerEntity customerEntity = mapper.customerToEntity(customer);

        // Mark entities as NOT new (this is an UPDATE operation)
        // This is critical for R2DBC Persistable pattern to work correctly
        personEntity.markAsNotNew();
        customerEntity.markAsNotNew();

        // Update Person first, then Customer
        // R2DBC will automatically increment the @Version field in CustomerEntity
        return personRepository.save(personEntity)
                .flatMap(updatedPerson -> {
                    // Set the person_id foreign key in customer entity
                    customerEntity.setPersonId(updatedPerson.getPersonId());
                    return customerRepository.save(customerEntity);
                })
                .flatMap(updatedCustomer -> findById(updatedCustomer.getCustomerId()))
                .doOnSuccess(updated -> log.debug("Customer updated successfully: {} - new version: {}",
                        updated.getCustomerId(), updated.getVersion()))
                .doOnError(error -> log.error("Error updating customer: {}", error.getMessage()));
    }

    @Override
    public Mono<Customer> findById(UUID customerId) {
        log.debug("Finding customer by ID: {}", customerId);

        return Mono.zip(
                personRepository.findById(customerId),
                customerRepository.findById(customerId)
        )
                .map(tuple -> {
                    PersonEntity personEntity = tuple.getT1();
                    CustomerEntity customerEntity = tuple.getT2();
                    return mapper.toDomain(personEntity, customerEntity);
                })
                .doOnSuccess(customer -> {
                    if (customer != null) {
                        log.debug("Customer found: {}", customerId);
                    }
                })
                .doOnError(error -> log.error("Error finding customer: {}", error.getMessage()));
    }

    @Override
    public Mono<Customer> findByIdentification(String identification) {
        log.debug("Finding customer by identification: {}", identification);

        return personRepository.findByIdentification(identification)
                .flatMap(personEntity -> customerRepository.findById(personEntity.getPersonId())
                        .map(customerEntity -> mapper.toDomain(personEntity, customerEntity))
                )
                .doOnSuccess(customer -> {
                    if (customer != null) {
                        log.debug("Customer found with identification: {}", identification);
                    }
                })
                .doOnError(error -> log.error("Error finding customer by identification: {}", error.getMessage()));
    }

    @Override
    public Flux<Customer> findAll(int page, int size) {
        log.debug("Finding all customers - page: {}, size: {}", page, size);

        PageRequest pageRequest = PageRequest.of(page, size);

        return customerRepository.findAllBy(pageRequest)
                .flatMap(customerEntity ->
                        personRepository.findById(customerEntity.getCustomerId())
                                .map(personEntity -> mapper.toDomain(personEntity, customerEntity))
                )
                .doOnComplete(() -> log.debug("Retrieved all customers"))
                .doOnError(error -> log.error("Error finding all customers: {}", error.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByIdentification(String identification) {
        log.debug("Checking if customer exists with identification: {}", identification);

        return personRepository.existsByIdentification(identification)
                .doOnSuccess(exists -> log.debug("Customer exists: {}", exists))
                .doOnError(error -> log.error("Error checking customer existence: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(UUID customerId) {
        log.debug("Deleting customer (hard delete): {}", customerId);

        // Hard delete: Remove customer and person from database
        // Delete Customer first (FK constraint), then Person
        return customerRepository.deleteById(customerId)
                .then(personRepository.deleteById(customerId))
                .doOnSuccess(v -> log.debug("Customer deleted successfully: {}", customerId))
                .doOnError(error -> log.error("Error deleting customer: {}", error.getMessage()));
    }

    @Override
    public Mono<Long> count() {
        log.debug("Counting all customers");

        return customerRepository.count()
                .doOnSuccess(count -> log.debug("Total customers: {}", count))
                .doOnError(error -> log.error("Error counting customers: {}", error.getMessage()));
    }

}
