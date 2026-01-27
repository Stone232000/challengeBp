package com.espiedra.api.account.service.infrastructure.adapter.input.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: Customer Event from Kafka
 * <p>
 * Represents a customer event published by api-customer-service.
 * Used for async communication between microservices.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer event data from Kafka messages
 * <p>
 * EVENT TYPES:
 * - customer.created: New customer registered
 * - customer.updated: Customer information updated
 * - customer.deleted: Customer deactivated/deleted
 * <p>
 * STRUCTURE: Flat structure matching the exact JSON published by customer-service
 * <p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEventDTO {

    /**
     * Event ID (unique for each event) - for idempotency
     */
    @JsonProperty("eventId")
    private String eventId;

    /**
     * Event type (customer.created, customer.updated, customer.deleted)
     */
    @JsonProperty("eventType")
    private String eventType;

    /**
     * Timestamp when event was created
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Correlation ID for distributed tracing
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * Customer unique identifier
     */
    @JsonProperty("customerId")
    private UUID customerId;

    /**
     * Customer full name
     */
    @JsonProperty("name")
    private String name;

    /**
     * Customer identification number (cedula, passport, etc.)
     */
    @JsonProperty("identification")
    private String identification;

    /**
     * Customer state (active/inactive)
     */
    @JsonProperty("state")
    private Boolean state;

    /**
     * Customer gender (optional)
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Customer age (optional)
     */
    @JsonProperty("age")
    private Integer age;

    /**
     * Customer address (optional)
     */
    @JsonProperty("address")
    private String address;

    /**
     * Customer phone number (optional)
     */
    @JsonProperty("phone")
    private String phone;

    /**
     * Creation timestamp (for created events)
     */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * Update timestamp (for updated events)
     */
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * Deletion timestamp (for deleted events)
     */
    @JsonProperty("deletedAt")
    private LocalDateTime deletedAt;
}
