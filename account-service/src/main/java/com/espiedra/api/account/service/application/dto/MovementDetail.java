package com.espiedra.api.account.service.application.dto;

import java.math.BigDecimal;

/**
 * DTO: Movement Detail
 * <p>
 * Detailed information about a single movement/transaction.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer movement data in reports
 * <p>
 */
public record MovementDetail(
    String date,
    String movementType,
    String description,
    BigDecimal amount,
    BigDecimal balance,
    String transactionId
) {}
