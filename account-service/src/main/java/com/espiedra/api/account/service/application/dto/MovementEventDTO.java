package com.espiedra.api.account.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: Movement Event for Kafka
 * <p>
 * Data Transfer Object for movement/transaction events published to Kafka.
 * Used for event-driven communication between microservices.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PATTERN: Event-Driven Architecture
 * PURPOSE: Serialize movement events to Kafka topics
 * <p>
 * KAFKA TOPIC: banking.movement.events
 * EVENT TYPES:
 * - movement.created
 * <p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementEventDTO {
    private String eventType;
    private UUID movementId;
    private Long accountNumber;
    private String movementType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String transactionId;
    private LocalDateTime timestamp;
}
