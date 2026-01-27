package com.espiedra.api.customer.service.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Topic Configuration
 * <p>
 * Automatically creates Kafka topics on application startup if they don't exist.
 * This ensures topics are properly configured with partitions and replication.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Factory Pattern: Creates Kafka topic beans
 * - Configuration Pattern: Externalizes topic configuration
 * <p>
 * FUNCTIONS:
 * - Topics are created declaratively
 * - Partition and replication settings are explicit
 * - Idempotent (safe to run multiple times)
 */
@Slf4j
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.customer-events}")
    private String customerEventsTopic;

    /**
     * KafkaAdmin bean for managing Kafka topics.
     * This bean is responsible for creating topics if they don't exist.
     *
     * @return KafkaAdmin
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Customer Events Topic
     * <p>
     * Topic for customer domain events (created, updated, deleted).
     * Uses hierarchical naming convention: banking.customer.events
     * <p>
     * Configuration:
     * - Partitions: 3 (for parallel processing)
     * - Replication: 1 (development), should be 3 in production
     * - Retention: 7 days (604800000 ms)
     * - Cleanup: delete (not compacted)
     * - Compression: lz4 (fast compression)
     *
     * @return NewTopic
     */
    @Bean
    public NewTopic customerEventsTopic() {
        log.info("Configuring Kafka topic: {}", customerEventsTopic);

        return TopicBuilder.name(customerEventsTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .config("compression.type", "lz4")
                .build();
    }
}
