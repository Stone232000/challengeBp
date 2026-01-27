package com.espiedra.api.account.service.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO: Account Statement
 * <p>
 * Statement for a single account including all movements.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer account statement data
 * <p>
 */
public record AccountStatement(
    Long accountNumber,
    String accountType,
    Boolean state,
    BigDecimal initialBalance,
    BigDecimal finalBalance,
    List<MovementDetail> movements
) {}

