package com.espiedra.api.account.service.infrastructure.adapter.input.rest.dto;

import java.math.BigDecimal;

/**
 * DTO: Movements Summary
 * Summary statistics for movements in a period
 */
public record MovementsSummary(
        String periodStart,
        String periodEnd,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        Integer creditCount,
        Integer debitCount,
        BigDecimal averageTransactionAmount,
        BigDecimal largestTransaction,
        BigDecimal smallestTransaction
) {}
