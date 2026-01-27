package com.espiedra.api.account.service.application.port.output;

import reactor.core.publisher.Mono;

/**
 * APPLICATION PORT (OUTPUT): EventPublisher
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining infrastructure capability
 * - Publisher Pattern: Abstract event publishing layer
 * - Observer Pattern: Event-driven communication
 * - Dependency Inversion Principle (DIP): Domain defines contract, infrastructure implements
 * <p>
 * SOLID PRINCIPLES:
 * - Dependency Inversion: High-level domain doesn't depend on low-level Kafka
 * - Interface Segregation: Specific interface for event publishing
 * - Single Responsibility: Only defines event publishing contract
 * - Open/Closed: Open for new event types, closed for modification
 * <p>
 * IMPLEMENTATION:
 * - Implemented by KafkaEventPublisher in infrastructure layer
 * - Uses Reactor Kafka for reactive event publishing
 * <p>
 * KAFKA TOPICS:
 * - banking.account.events (for account CRUD events)
 * - banking.movement.events (for movement events)
 * <p>
 * EVENT HEADERS:
 * - correlation-id: For distributed tracing
 * - event-type: Type of event (ACCOUNT_CREATED, etc.)
 * - timestamp: Event timestamp
 * <p>
 */
public interface EventPublisherPort {

    /**
     * Publish an event to Kafka
     * <p>
     * FIRE-AND-FORGET PATTERN:
     * - Events are published asynchronously
     * - No blocking on event publishing
     * - Errors are logged but don't block main flow
     *
     * @param topic Kafka topic name
     * @param key Event key (typically entity ID)
     * @param event Event payload
     * @param <T> Event type
     * @return Mono<Void> Completion signal (fire-and-forget)
     */
    <T> Mono<Void> publish(String topic, String key, T event);

    /**
     * Publish an event with correlation ID for distributed tracing
     * <p>
     * @param topic Kafka topic name
     * @param key Event key
     * @param event Event payload
     * @param correlationId Correlation ID for tracing
     * @param <T> Event type
     * @return Mono<Void> Completion signal
     */
    <T> Mono<Void> publishWithCorrelation(String topic, String key, T event, String correlationId);
}
