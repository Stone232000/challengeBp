package com.espiedra.api.account.service.infrastructure.adapter.output.event;

import com.espiedra.api.account.service.application.port.output.EventPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * INFRASTRUCTURE ADAPTER (OUTPUT): KafkaEventPublisher
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts Kafka to domain's EventPublisher port
 * - Fire-and-Forget Pattern: Non-blocking async event publishing
 * - Event-Driven Architecture: Domain events published to Kafka topics
 * - Publisher-Subscriber Pattern: Kafka-based event distribution
 * - Dependency Inversion Principle (DIP): Implements domain-defined contract
 * <p>
 * SOLID PRINCIPLES:
 * - Dependency Inversion: Depends on domain port (EventPublisher), not concrete implementation
 * - Single Responsibility: Only handles Kafka event publishing
 * - Open/Closed: Open for new event types, closed for modification
 * - Interface Segregation: Implements only event publishing interface
 * - Liskov Substitution: Can be replaced by any EventPublisher implementation
 * <p>
 * This adapter enables ASYNCHRONOUS COMMUNICATION between microservices:
 * <p>
 * CONFIGURATION REQUIREMENTS:
 * - KafkaSender bean configured in KafkaConfig
 * - ObjectMapper bean for JSON serialization
 * - Kafka bootstrap servers in application.yaml
 * <p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisherPort implements EventPublisherPort {

    /**
     * Reactor Kafka sender for reactive event publishing
     * Configured in KafkaConfig with producer properties
     */
    private final KafkaSender<String, String> kafkaSender;

    /**
     * Jackson ObjectMapper for JSON serialization
     * Converts event objects to JSON strings
     */
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTATION NOTES:
     * - Delegates to publishWithCorrelation with null correlation ID
     * - Maintains clean API for simple event publishing
     *
     * @param topic Kafka topic name (e.g., "banking.account.events")
     * @param key Event key (typically entity ID, used for partitioning)
     * @param event Event payload (will be serialized to JSON)
     * @param <T> Event type (any serializable object)
     * @return Mono<Void> Completion signal (completes immediately on fire-and-forget)
     */
    @Override
    public <T> Mono<Void> publish(String topic, String key, T event) {
        log.debug("Publishing event to topic '{}' with key '{}'", topic, key);
        return publishWithCorrelation(topic, key, event, null);
    }

    /**
     * @param topic Kafka topic name
     * @param key Event key (for partitioning and ordering)
     * @param event Event payload (any serializable object)
     * @param correlationId Distributed tracing correlation ID (nullable)
     * @param <T> Event type
     * @return Mono<Void> Completion signal
     */
    @Override
    public <T> Mono<Void> publishWithCorrelation(String topic, String key, T event, String correlationId) {
        try {
            // STEP 1: Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            log.trace("Serialized event to JSON: {}", eventJson);

            // STEP 2: Create Kafka ProducerRecord
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, eventJson);

            // STEP 3: Add event headers (metadata)
            // Header 1: Timestamp (when event was created)
            producerRecord.headers().add(
                new RecordHeader("timestamp", Instant.now().toString().getBytes(StandardCharsets.UTF_8))
            );

            // Header 2: Event type (for routing/filtering by consumers)
            producerRecord.headers().add(
                new RecordHeader("event-type", event.getClass().getSimpleName().getBytes(StandardCharsets.UTF_8))
            );

            // Header 3: Correlation ID (for distributed tracing, if present)
            if (correlationId != null && !correlationId.isBlank()) {
                producerRecord.headers().add(
                    new RecordHeader("x-correlation-id", correlationId.getBytes(StandardCharsets.UTF_8))
                );
                log.debug("Added x-correlation-id header: {}", correlationId);
            }

            // STEP 4: Wrap in SenderRecord (Reactor Kafka wrapper)
            // Metadata parameter (key) allows tracking send results
            SenderRecord<String, String, String> senderRecord = SenderRecord.create(producerRecord, key);

            // STEP 5: Send to Kafka (reactive, non-blocking)
            return kafkaSender.send(Mono.just(senderRecord))
                // Log successful send
                .doOnNext(result -> {
                    log.info("Event published successfully to topic '{}': key='{}', partition={}, offset={}",
                        topic, key,
                        result.recordMetadata().partition(),
                        result.recordMetadata().offset());
                })
                // Log send errors
                .doOnError(error -> {
                    log.error("Error publishing event to topic '{}' with key '{}': {}",
                        topic, key, error.getMessage(), error);
                })
                // Complete the Mono<Void>
                .then()
                // FIRE-AND-FORGET: Swallow errors, don't fail main operation
                .onErrorResume(error -> {
                    log.warn("Event publishing failed but continuing execution (fire-and-forget): topic='{}', key='{}'",
                        topic, key);
                    return Mono.empty();
                });

        } catch (JsonProcessingException e) {
            // JSON serialization error
            log.error("Error serializing event to JSON: class={}, error={}",
                event.getClass().getSimpleName(), e.getMessage(), e);
            // Fire-and-forget: Return empty Mono, don't fail main operation
            return Mono.empty();
        }
    }
}
