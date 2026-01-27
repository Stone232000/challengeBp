package com.espiedra.api.customer.service.application.service;

import com.espiedra.api.customer.service.application.dto.CustomerCreationResult;
import com.espiedra.api.customer.service.application.dto.CustomerEventDTO;
import com.espiedra.api.customer.service.application.ports.input.CreateCustomerUseCase;
import com.espiedra.api.customer.service.application.ports.input.DeleteCustomerUseCase;
import com.espiedra.api.customer.service.application.ports.input.GetCustomerUseCase;
import com.espiedra.api.customer.service.application.ports.input.UpdateCustomerUseCase;
import com.espiedra.api.customer.service.application.ports.output.CustomerRepositoryPort;
import com.espiedra.api.customer.service.application.ports.output.EventPublisherPort;
import com.espiedra.api.customer.service.application.ports.output.JwtTokenPort;
import com.espiedra.api.customer.service.application.ports.output.PasswordEncoderPort;
import com.espiedra.api.customer.service.domain.exception.CustomerAlreadyExistsException;
import com.espiedra.api.customer.service.domain.exception.CustomerNotFoundException;
import com.espiedra.api.customer.service.domain.exception.InvalidPasswordException;
import com.espiedra.api.customer.service.domain.exception.OptimisticLockException;
import com.espiedra.api.customer.service.domain.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Service - Application Layer
 * <p>
 * This service implements all customer use cases by orchestrating domain logic
 * and coordinating with output ports (repository, event publisher, etc.).
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * - Implements INPUT PORTS (use case interfaces)
 * - Uses OUTPUT PORTS (repository, event publisher, password encoder, JWT)
 * - Contains application logic, NOT domain logic
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Orchestrates customer operations
 * - Dependency Inversion Principle (DIP): Depends on abstractions (ports), not implementations
 * - Interface Segregation Principle (ISP): Implements multiple focused use case interfaces
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Service Pattern: Application service that orchestrates business operations
 * - Facade Pattern: Provides unified interface to multiple use cases
 * - Transaction Script Pattern: Each method represents a transaction
 */
@Slf4j
@Service
@RequiredArgsConstructor // SOLID: Dependency Injection via constructor (DIP)
public class CustomerService implements
        CreateCustomerUseCase,
        GetCustomerUseCase,
        UpdateCustomerUseCase,
        DeleteCustomerUseCase {

    // Output Ports (injected via constructor)
    private final CustomerRepositoryPort customerRepository;
    private final EventPublisherPort eventPublisher;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtTokenPort jwtToken;

    // =====================================================
    // CREATE CUSTOMER USE CASE
    // =====================================================

    /**
     * Create a new customer.
     * <p>
     * TRANSACTION FLOW:
     * 1. Validate customer doesn't exist (by identification)
     * 2. Encode password with BCrypt
     * 3. Create customer entity
     * 4. Save to database
     * 5. Generate JWT token
     * 6. Publish banking.customer.created. event to Kafka
     * 7. Return customer + JWT token
     */
    @Override
    public Mono<CustomerCreationResult> createCustomer(Customer customer, String rawPassword, String correlationId) {
        log.info("Creating customer with identification: {} and correlation-id: {}",
                customer.getIdentification(), correlationId);

        return customerRepository.existsByIdentification(customer.getIdentification())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Customer already exists with identification: {}", customer.getIdentification());
                        return Mono.error(new CustomerAlreadyExistsException(customer.getIdentification()));
                    }

                    // Encode password
                    String encodedPassword = passwordEncoder.encode(rawPassword);

                    // Create customer entity with encoded password
                    // Customer extends Person, so we pass all Person fields
                    Customer newCustomer = Customer.create(
                            customer.getName(),
                            customer.getGender(),
                            customer.getAge(),
                            customer.getIdentification(),
                            customer.getAddress(),
                            customer.getPhone(),
                            encodedPassword
                    );

                    // Save customer
                    return customerRepository.save(newCustomer)
                            .flatMap(savedCustomer -> {
                                log.info("Customer created successfully: {}", savedCustomer.getCustomerId());

                                // Generate JWT token
                                String token = jwtToken.generateToken(
                                        savedCustomer.getCustomerId(),
                                        savedCustomer.getIdentification()
                                );

                                // Publish customer.created event with correlation-id for distributed tracing
                                publishCustomerEvent(savedCustomer, "created", correlationId).subscribe();

                                return Mono.just(new CustomerCreationResult(savedCustomer, token));
                            });
                })
                .doOnError(error -> log.error("Error creating customer: {}", error.getMessage()));
    }

    // =====================================================
    // GET CUSTOMER USE CASES
    // =====================================================

    @Override
    public Mono<Customer> getCustomerById(UUID customerId) {
        log.info("Getting customer by ID: {}", customerId);

        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .doOnSuccess(customer -> log.info("Customer by ID found: {}", customerId))
                .doOnError(error -> log.error("Error getting customer by ID: {}", error.getMessage()));
    }

    @Override
    public Mono<Customer> getCustomerByIdentification(String identification) {
        log.info("Getting customer by identification: {}", identification);

        return customerRepository.findByIdentification(identification)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(identification)))
                .doOnSuccess(customer -> log.info("Customer found: {}", identification))
                .doOnError(error -> log.error("Error getting customer: {}", error.getMessage()));
    }

    @Override
    public Flux<Customer> getAllCustomers(int page, int size) {
        log.info("Getting all customers - page: {}, size: {}", page, size);

        return customerRepository.findAll(page, size)
                .doOnComplete(() -> log.info("Retrieved customers successfully"))
                .doOnError(error -> log.error("Error getting customers: {}", error.getMessage()));
    }

    @Override
    public Mono<Long> count() {
        log.info("Counting all customers");

        return customerRepository.count()
                .switchIfEmpty(Mono.just(0L))
                .doOnSuccess(count -> log.info("Total customers found: {}", count))
                .doOnError(error -> log.error("Error counting customers: {}", error.getMessage()));
    }

    // =====================================================
    // UPDATE CUSTOMER USE CASES
    // =====================================================

    @Override
    public Mono<Customer> updateCustomer(UUID customerId, Customer updatedCustomer, Long expectedVersion, String correlationId) {
        log.info("Updating customer: {} with correlation-id: {}", customerId, correlationId);

        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(existingCustomer -> {
                    // Validate optimistic locking
                    if (!existingCustomer.getVersion().equals(expectedVersion)) {
                        return Mono.error(new OptimisticLockException(
                                customerId, expectedVersion, existingCustomer.getVersion()
                        ));
                    }

                    // Update customer fields (Customer extends Person, so we update inherited fields)
                    Customer customerToUpdate = existingCustomer.toBuilder()
                            .name(updatedCustomer.getName())
                            .gender(updatedCustomer.getGender())
                            .age(updatedCustomer.getAge())
                            .address(updatedCustomer.getAddress())
                            .phone(updatedCustomer.getPhone())
                            .state(updatedCustomer.getState())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return customerRepository.update(customerToUpdate)
                            .flatMap(updated -> {
                                log.info("Customer updated successfully: {}", customerId);

                                // Publish customer.updated event (fire-and-forget)
                                publishCustomerEvent(updated, "updated", correlationId).subscribe();

                                return Mono.just(updated);
                            });
                })
                .doOnError(error -> log.error("Error updating customer: {}", error.getMessage()));
    }

    @Override
    public Mono<Customer> updatePassword(UUID customerId, String currentPassword, String newPassword, Long expectedVersion, String correlationId) {
        log.info("Updating password for customer: {} with correlation-id: {}", customerId, correlationId);

        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(existingCustomer -> {
                    // Validate optimistic locking
                    if (!existingCustomer.getVersion().equals(expectedVersion)) {
                        return Mono.error(new OptimisticLockException(
                                customerId, expectedVersion, existingCustomer.getVersion()
                        ));
                    }

                    // Validate current password
                    if (!passwordEncoder.matches(currentPassword, existingCustomer.getPassword())) {
                        return Mono.error(new InvalidPasswordException("Current password is incorrect"));
                    }

                    // Encode new password
                    String encodedNewPassword = passwordEncoder.encode(newPassword);

                    // Update password using domain method
                    Customer customerToUpdate = existingCustomer.updatePassword(encodedNewPassword);

                    return customerRepository.update(customerToUpdate)
                            .doOnSuccess(updated -> log.info("Password updated successfully for customer: {}", customerId));
                })
                .doOnError(error -> log.error("Error updating password: {}", error.getMessage()));
    }

    @Override
    public Mono<Customer> activateCustomer(UUID customerId, Long expectedVersion, String correlationId) {
        log.info("Activating customer: {} with correlation-id: {}", customerId, correlationId);

        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(existingCustomer -> {
                    // Validate optimistic locking
                    if (!existingCustomer.getVersion().equals(expectedVersion)) {
                        return Mono.error(new OptimisticLockException(
                                customerId, expectedVersion, existingCustomer.getVersion()
                        ));
                    }

                    // Activate using domain method
                    Customer customerToUpdate = existingCustomer.activate();

                    return customerRepository.update(customerToUpdate)
                            .flatMap(updated -> {
                                log.info("Customer activated successfully: {}", customerId);

                                // Publish customer.updated event
                                publishCustomerEvent(updated, "updated", correlationId).subscribe();

                                return Mono.just(updated);
                            });
                })
                .doOnError(error -> log.error("Error activating customer: {}", error.getMessage()));
    }

    @Override
    public Mono<Customer> deactivateCustomer(UUID customerId, Long expectedVersion, String correlationId) {
        log.info("Deactivating customer: {} with correlation-id: {}", customerId, correlationId);

        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(existingCustomer -> {
                    // Validate optimistic locking
                    if (!existingCustomer.getVersion().equals(expectedVersion)) {
                        return Mono.error(new OptimisticLockException(
                                customerId, expectedVersion, existingCustomer.getVersion()
                        ));
                    }

                    // Deactivate using domain method
                    Customer customerToUpdate = existingCustomer.deactivate();

                    return customerRepository.update(customerToUpdate)
                            .flatMap(updated -> {
                                log.info("Customer deactivated successfully: {}", customerId);

                                // Publish customer.updated event
                                publishCustomerEvent(updated, "updated", correlationId).subscribe();

                                return Mono.just(updated);
                            });
                })
                .doOnError(error -> log.error("Error deactivating customer: {}", error.getMessage()));
    }

    // =====================================================
    // DELETE CUSTOMER USE CASES
    // =====================================================

    @Override
    public Mono<Void> deleteCustomer(UUID customerId, String correlationId) {
        log.info("Deleting customer (hard delete): {} with correlation-id: {}", customerId, correlationId);

        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(customer -> customerRepository.deleteById(customerId)
                        .doOnSuccess(v -> {
                            log.info("Customer deleted successfully: {}", customerId);
                            // Publish customer.deleted event (fire-and-forget)
                            publishCustomerEvent(customer, "deleted", correlationId).subscribe();
                        })
                )
                .doOnError(error -> log.error("Error deleting customer: {}", error.getMessage()));
    }

    // =====================================================
    // PRIVATE EVENT PUBLISHING METHODS
    // =====================================================

    /**
     * Publish customer events to Kafka.
     * DESIGN PATTERN: Event-Driven Architecture
     *
     * @param customer      Customer domain model
     * @param eventType     Type of event (created, updated, deleted)
     * @param correlationId Correlation ID from HTTP header for distributed tracing
     */
    private Mono<Void> publishCustomerEvent(Customer customer, String eventType, String correlationId) {
        CustomerEventDTO.CustomerEventDTOBuilder eventBuilder = CustomerEventDTO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("customer." + eventType)
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .identification(customer.getIdentification());

        // Set event-specific fields
        switch (eventType) {
            case "created":
                eventBuilder.state(customer.getState()).createdAt(customer.getCreatedAt());
                break;
            case "updated":
                eventBuilder.state(customer.getState()).updatedAt(customer.getUpdatedAt());
                break;
            case "deleted":
                eventBuilder.deletedAt(LocalDateTime.now());
                break;
            default:
                return Mono.error(new IllegalArgumentException("Unknown event type: " + eventType));
        }

        CustomerEventDTO event = eventBuilder.build();

        return eventPublisher.publishCustomerEvent(event)
                .doOnSuccess(v -> log.info("Published customer.{} event: {} with correlation-id: {}",
                        eventType, customer.getCustomerId(), correlationId))
                .doOnError(error -> log.error("Error publishing customer.{} event: {}", eventType, error.getMessage()));
    }

}
