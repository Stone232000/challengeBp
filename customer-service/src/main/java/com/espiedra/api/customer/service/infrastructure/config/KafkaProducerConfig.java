package com.espiedra.api.customer.service.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

/**
 * Kafka Producer Configuration - Reactive
 * <p>
 * Configures ReactiveKafkaProducerTemplate for publishing events to Kafka.
 * Uses Reactor Kafka for non-blocking, reactive event publishing.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only configures Kafka producer
 * - Dependency Inversion Principle (DIP): Uses KafkaProperties abstraction
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Factory Pattern: Creates ReactiveKafkaProducerTemplate bean
 * - Configuration Pattern: Externalizes configuration to application.yaml
 */
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * Create ReactiveKafkaProducerTemplate bean.
     * <p>
     * Configuration is loaded from application.yaml:
     * - bootstrap-servers
     * - key-serializer
     * - value-serializer
     * - acks
     * - retries
     * - enable.idempotence
     * - compression.type.
     *
     * @return ReactiveKafkaProducerTemplate<String, String>
     */
    @Bean
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate() {
        Map<String, Object> producerProps = kafkaProperties.buildProducerProperties(null);

        // Ensure serializers are set (String for both key and value)
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }
}
