package com.espiedra.api.account.service.infrastructure.adapter.input.rest.dto;

import java.math.BigDecimal;

/**
 * DTO: Balance Response
 * <p>
 * Response for balance query endpoints.
 * Returns current and available balance for an account.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer balance information via REST API
 * <p>
 */
public record BalanceResponse(
    Long accountNumber,
    BigDecimal balance,
    BigDecimal availableBalance
) {}
