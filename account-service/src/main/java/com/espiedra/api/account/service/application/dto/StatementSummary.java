package com.espiedra.api.account.service.application.dto;

import java.math.BigDecimal;

/**
 * DTO: Statement Summary
 * <p>
 * Summary statistics for account statement report.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer summary statistics
 * <p>
 */
public record StatementSummary(
    Integer totalAccounts,
    BigDecimal totalCredits,
    BigDecimal totalDebits,
    Integer totalMovements,
    BigDecimal netChange
) {}
