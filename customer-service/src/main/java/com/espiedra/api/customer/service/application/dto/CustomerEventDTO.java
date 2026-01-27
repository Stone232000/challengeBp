package com.espiedra.api.customer.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Event DTO - Application Layer
 * <p>
 * Data Transfer Object for customer events published to Kafka.
 * This DTO carries event information between application and infrastructure layers.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only represents event data structure
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Data Transfer Object (DTO) Pattern: Transfers data between layers
 * - Builder Pattern: Provides fluent API for object construction
 * <p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEventDTO {

    /**
     * Unique event identifier (UUID)
     */
    private String eventId;

    /**
     * Event type identifier (customer.created, customer.updated., customer.deleted.)
     */
    private String eventType;

    /**
     * Timestamp when the event occurred
     */
    private LocalDateTime timestamp;

    /**
     * Correlation ID for distributed tracing across services
     * Propagated from HTTP X-Correlation-Id header
     */
    private String correlationId;

    /**
     * Customer unique identifier
     */
    private UUID customerId;

    /**
     * Customer name
     */
    private String name;

    /**
     * Customer identification number
     */
    private String identification;

    /**
     * Customer state (active/inactive)
     */
    private Boolean state;

    /**
     * Timestamp when customer was created (for created events)
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when customer was last updated (for updated events)
     */
    private LocalDateTime updatedAt;

    /**
     * Timestamp when customer was deleted (for deleted events)
     */
    private LocalDateTime deletedAt;
}
