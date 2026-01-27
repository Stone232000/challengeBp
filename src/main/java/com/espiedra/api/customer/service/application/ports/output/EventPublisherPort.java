package com.espiedra.api.customer.service.application.ports.output;

import reactor.core.publisher.Mono;

/**
 * Output Port (Event Publisher Interface) - Kafka Event Publisher
 * <p>
 * HEXAGONAL ARCHITECTURE - SECONDARY PORT:
 * This interface defines the contract for publishing domain events to Kafka.
 * It is independent of any framework or infrastructure (Reactor Kafka, Spring Kafka, etc.).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Domain depends on abstraction, not Kafka implementation
 * - Interface Segregation Principle (ISP): Focused interface for event publishing
 * - Single Responsibility Principle (SRP): Only handles event publishing
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: This is a SECONDARY PORT (output) in hexagonal architecture
 * - Publisher Pattern: Publishes domain events to external systems
 * - Event-Driven Architecture: Decouples services via asynchronous events
 */
public interface EventPublisherPort {

    /**
     * Publish a domain event to Kafka.
     *
     * @param topic Kafka topic name
     * @param key Message key (for partitioning)
     * @param event Event payload (will be serialized to JSON)
     * @param <T> Event type
     * @return Mono<Void> when publish completes
     */
    <T> Mono<Void> publish(String topic, String key, T event);

    /**
     * Publish a customer event to customer.events. topic.
     *
     * @param event Customer event payload
     * @param <T> Event type
     * @return Mono<Void> when publish completes
     */
    <T> Mono<Void> publishCustomerEvent(T event);
}
