package com.espiedra.api.customer.service.infrastructure.adapter.output.event;

import com.espiedra.api.customer.service.application.ports.output.EventPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kafka Event Publisher Adapter - Infrastructure Layer
 * <p>
 * This adapter implements the EventPublisherPort (output port) using Reactor Kafka.
 * It publishes domain events to Kafka topics in a reactive, non-blocking manner.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is a SECONDARY ADAPTER that implements an OUTPUT PORT.
 * It adapts the domain event publishing to Kafka infrastructure.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Implements port interface from application layer
 * - Single Responsibility Principle (SRP): Only handles event publishing to Kafka
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts Reactor Kafka to domain port interface
 * - Publisher Pattern: Publishes events to message broker
 * - Fire-and-Forget Pattern: Events are published asynchronously
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private final ReactiveKafkaProducerTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.customer-events}")
    private String customerEventsTopic;

    @Value("${spring.application.name}")
    private String applicationName;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public <T> Mono<Void> publish(String topic, String key, T event) {
        try {
            // Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);

            // Build Kafka headers
            List<Header> headers = buildKafkaHeaders(event);

            // Create ProducerRecord with headers
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    null, // partition will be determined by key
                    key,
                    eventJson,
                    headers
            );

            log.debug("Publishing event to topic: {} with key: {} and {} headers", topic, key, headers.size());

            return kafkaTemplate.send(record)
                    .doOnSuccess(result -> log.info(
                            "Event published successfully to topic: {} partition: {} offset: {} with headers: {}",
                            topic,
                            result.recordMetadata().partition(),
                            result.recordMetadata().offset(),
                            headers.size()
                    ))
                    .doOnError(error -> log.error(
                            "Error publishing event to topic: {} - Error: {}",
                            topic,
                            error.getMessage()
                    ))
                    .then();

        } catch (JsonProcessingException e) {
            log.error("Error serializing event to JSON: {}", e.getMessage());
            return Mono.error(new RuntimeException("Failed to serialize event", e));
        }
    }

    @Override
    public <T> Mono<Void> publishCustomerEvent(T event) {
        log.debug("Publishing customer event to topic: {}", customerEventsTopic);

        // Extract identification from event for partitioning
        // Events with same identification go to same partition (ordering guarantee)
        String key = extractKeyFromEvent(event);

        return publish(customerEventsTopic, key, event);
    }

    /**
     * Build Kafka headers for event metadata.
     * Headers provide additional information without parsing the message payload.
     * <p>
     * KAFKA HEADERS BEST PRACTICES:
     * - event-id: Unique identifier for idempotency
     * - event-type: Event classification for routing/filtering
     * - event-timestamp: When the event occurred (business time)
     * - source: Origin microservice for traceability
     * - x-correlation-id: Distributed tracing across services
     * - content-type: Message format (application/json)
     * - schema-version: For schema evolution
     *
     * @param event Event object
     * @return List of Kafka headers
     */
    private <T> List<Header> buildKafkaHeaders(T event) {
        List<Header> headers = new ArrayList<>();

        try {
            // Extract event metadata using reflection or JSON parsing
            String eventJson = objectMapper.writeValueAsString(event);
            var eventNode = objectMapper.readTree(eventJson);

            // Header: event-id (for idempotency and deduplication)
            String eventId = eventNode.has("eventId")
                    ? eventNode.get("eventId").asText()
                    : UUID.randomUUID().toString();
            headers.add(new RecordHeader("event-id", eventId.getBytes(StandardCharsets.UTF_8)));

            // Header: event-type (for event routing and filtering)
            String eventType = eventNode.has("eventType")
                    ? eventNode.get("eventType").asText()
                    : "unknown";
            headers.add(new RecordHeader("event-type", eventType.getBytes(StandardCharsets.UTF_8)));

            // Header: event-timestamp (business event time)
            String timestamp = eventNode.has("timestamp")
                    ? eventNode.get("timestamp").asText()
                    : LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            headers.add(new RecordHeader("event-timestamp", timestamp.getBytes(StandardCharsets.UTF_8)));

            // Header: source (originating microservice)
            headers.add(new RecordHeader("source", applicationName.getBytes(StandardCharsets.UTF_8)));

            // Header: correlation-id (for distributed tracing)
            String correlationId = eventNode.has("correlationId")
                    ? eventNode.get("correlationId").asText()
                    : eventId; // Use event-id as fallback
            headers.add(new RecordHeader("x-correlation-id", correlationId.getBytes(StandardCharsets.UTF_8)));

            // Header: content-type (message format)
            headers.add(new RecordHeader("content-type", "application/json".getBytes(StandardCharsets.UTF_8)));

            // Header: schema-version (for schema evolution)
            headers.add(new RecordHeader("schema-version", "1.0".getBytes(StandardCharsets.UTF_8)));

            // Header: entity-id (customer ID for entity-specific processing)
            if (eventNode.has("customerId")) {
                String customerId = eventNode.get("customerId").asText();
                headers.add(new RecordHeader("entity-id", customerId.getBytes(StandardCharsets.UTF_8)));
            }

            log.debug("Built {} Kafka headers for event type: {}", headers.size(), eventType);

        } catch (Exception e) {
            log.warn("Error building Kafka headers, using minimal headers: {}", e.getMessage());
            // Fallback: minimal headers
            headers.add(new RecordHeader("event-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            headers.add(new RecordHeader("source", applicationName.getBytes(StandardCharsets.UTF_8)));
            headers.add(new RecordHeader("content-type", "application/json".getBytes(StandardCharsets.UTF_8)));
        }

        return headers;
    }

    /**
     * Extract key from event for Kafka partitioning.
     * Uses customer identification to ensure ordering guarantee:
     * - Same customer events go to same partition
     * - Events are processed in order per customer
     * <p>
     * PARTITIONING STRATEGY:
     * - Key = customer identification (e.g., "1234567890")
     * - Kafka uses hash(key) % partitions to determine partition
     * - Same key = same partition = ordering guarantee
     *
     * @param event Event object
     * @return Partition key (customer identification or fallback)
     */
    private <T> String extractKeyFromEvent(T event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            var eventNode = objectMapper.readTree(eventJson);

            // Try to extract customer identification (best key for partitioning)
            if (eventNode.has("identification")) {
                return eventNode.get("identification").asText();
            }

            // Fallback: use customerId
            if (eventNode.has("customerId")) {
                return eventNode.get("customerId").asText();
            }

            // Last fallback: use eventId
            if (eventNode.has("eventId")) {
                return eventNode.get("eventId").asText();
            }

        } catch (Exception e) {
            log.warn("Error extracting key from event, using timestamp: {}", e.getMessage());
        }

        // Ultimate fallback: timestamp (not ideal for ordering)
        return String.valueOf(System.currentTimeMillis());
    }
}
