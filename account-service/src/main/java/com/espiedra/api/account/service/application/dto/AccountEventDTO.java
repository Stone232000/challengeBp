package com.espiedra.api.account.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: Account Event for Kafka
 * <p>
 * Data Transfer Object for account events published to Kafka.
 * Used for event-driven communication between microservices.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PATTERN: Event-Driven Architecture
 * PURPOSE: Serialize account events to Kafka topics
 * <p>
 * KAFKA TOPIC: banking.account.events
 * EVENT TYPES:
 * - account.created
 * - account.updated
 * - account.deleted
 * <p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEventDTO {
    private String eventType;
    private Long accountNumber;
    private UUID customerId;
    private String accountType;
    private BigDecimal initialBalance;
    private BigDecimal balance;
    private Boolean state;
    private LocalDateTime timestamp;
}
