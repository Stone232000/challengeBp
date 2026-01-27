package com.espiedra.api.account.service.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * INFRASTRUCTURE CONFIGURATION: KafkaConfig
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Factory Pattern: Creates and configures Kafka beans
 * - Configuration Pattern: Centralized Kafka configuration
 * - Dependency Injection: Beans injected into adapters
 * - Reactive Streams: Non-blocking I/O with Reactor Kafka
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles Kafka configuration
 * - Dependency Inversion: Adapters depend on abstractions (KafkaSender/Receiver)
 * - Open/Closed: Open for new topics/configurations, closed for modification
 * <p>
 * PRODUCER (KafkaSender):
 * - Publishes events to topics
 * - Events serialized to bytes (String JSON in this case)
 * - Producer config: Bootstrap servers, serializers, acks, retries
 * <p>
 * CONSUMER (KafkaReceiver):
 * - Subscribes to topics and consumes events
 * - Events deserialized from bytes
 * - Consumer config: Bootstrap servers, group ID, deserializers, offset strategy
 * <p>
 * TOPICS USED:
 * ============
 * PRODUCER TOPICS (Published by Account Service):
 * - banking.account.events: Account CRUD events
 * - banking.movement.events: Movement events
 * <p>
 * CONSUMER TOPICS (Consumed by Account Service):
 * - banking.customer.events: Customer events from Customer Service
 * <p>
 * KafkaSender:
 * - Reactive producer wrapper
 * - Returns Flux<SenderResult> for send operations
 * - Non-blocking sends (doesn't block thread pool)
 * <p>
 * KafkaReceiver:
 * - Reactive consumer wrapper
 * - Returns Flux<ReceiverRecord> of events
 * - Auto-manages connections and offsets
 *
 */
@Slf4j
@Configuration
public class KafkaConfig {

    /**
     * Kafka bootstrap servers (broker addresses)
     * Injected from application.yaml: spring.kafka.bootstrap-servers
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Consumer group ID for Kafka consumer
     * Injected from application.yaml: spring.kafka.consumer.group-id
     * Example: "account-service-group"
     * <p>
     * CONSUMER GROUP:
     * - Multiple instances share partitions (load balancing)
     * - Each partition consumed by one instance (ordered processing)
     * - Offsets stored per group (independent progress tracking)
     */
    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    /**
     * Kafka topic for customer events
     * Injected from application.yaml: app.kafka.topics.customer-events
     * Default: "banking.customer.events"
     * <p>
     * TOPIC PURPOSE:
     * - Published by Customer Service on CRUD operations
     * - Consumed by Account Service
     * - Events: customer.created, customer.updated, customer.deleted
     */
    @Value("${app.kafka.topics.customer-events:banking.customer.events}")
    private String customerEventsTopic;

    /**
     * Create KafkaSender bean for reactive event publishing
     *
     * @return KafkaSender<String, String> Configured Kafka sender
     */
    @Bean
    public KafkaSender<String, String> kafkaSender() {
        log.info("Configuring KafkaSender with bootstrap servers: {}", bootstrapServers);

        // Producer configuration properties
        Map<String, Object> producerProps = new HashMap<>();

        // REQUIRED: Kafka broker addresses
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // SERIALIZERS: Convert keys/values to bytes
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // RELIABILITY: Wait for all replicas to acknowledge
        // Options: "0" (no wait), "1" (leader only), "all" (all replicas)
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        // RELIABILITY: Retry failed sends (transient failures)
        // Retries with exponential backoff (default: 100ms base, up to 32s)
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        // RELIABILITY: Enable idempotence (prevent duplicate events on retry)
        // Kafka assigns sequence numbers to detect duplicates
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // PERFORMANCE: Compress events for bandwidth efficiency
        // Options: "none", "gzip", "snappy", "lz4", "zstd"
        // Snappy: Fast compression, good ratio (recommended)
        producerProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // PERFORMANCE: Parallel requests per connection
        // With idempotence=true, max is 5 (ensures ordering)
        producerProps.put("max.in.flight.requests.per.connection", 5);

        // PERFORMANCE: Batch small events (default: 16KB)
        // Events batched for efficiency (reduces network overhead)
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // PERFORMANCE: Wait up to 10ms for batch to fill
        // Balances latency vs throughput
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        // Create SenderOptions from properties
        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);

        // Create and return KafkaSender
        KafkaSender<String, String> sender = KafkaSender.create(senderOptions);
        log.info("KafkaSender configured successfully");

        return sender;
    }

    /**
     * Create KafkaReceiver bean for reactive event consumption
     *
     * @return KafkaReceiver<String, String> Configured Kafka receiver
     */
    @Bean
    public KafkaReceiver<String, String> kafkaReceiver() {
        log.info("Configuring KafkaReceiver with bootstrap servers: {}, group ID: {}, topic: {}",
            bootstrapServers, consumerGroupId, customerEventsTopic);

        // Consumer configuration properties
        Map<String, Object> consumerProps = new HashMap<>();

        // REQUIRED: Kafka broker addresses
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // REQUIRED: Consumer group ID (for load balancing and offset tracking)
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        // DESERIALIZERS: Convert bytes to keys/values
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // OFFSET: Where to start if no offset stored
        // Options: "earliest" (from beginning), "latest" (from end)
        // earliest: Ensures no events are missed for new consumer groups
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // OFFSET: Disable auto-commit (manual commit for control)
        // Manual commit after successful processing (at-least-once delivery)
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // PERFORMANCE: Fetch up to 100 events per poll
        // Higher value: Better throughput, higher latency
        // Lower value: Lower throughput, lower latency
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        // RELIABILITY: Session timeout (consumer considered dead if no heartbeat)
        // Default: 10s (safe value for most cases)
        consumerProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);

        // RELIABILITY: Heartbeat interval (frequency of heartbeats to broker)
        // Should be < session.timeout.ms / 3
        // Default: 3s (sends heartbeat every 3s)
        consumerProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create ReceiverOptions from properties
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(consumerProps)
            // Subscribe to customer events topic
            .subscription(Collections.singleton(customerEventsTopic));

        // Create and return KafkaReceiver
        KafkaReceiver<String, String> receiver = KafkaReceiver.create(receiverOptions);
        log.info("KafkaReceiver configured successfully");

        return receiver;
    }
}
