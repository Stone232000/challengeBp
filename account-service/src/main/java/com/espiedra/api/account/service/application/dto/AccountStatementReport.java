package com.espiedra.api.account.service.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO: Account Statement Report
 * <p>
 * Complete account statement report for a customer.
 * Contains all accounts, movements, and summary statistics.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer structured report data between layers
 * <p>
 */
public record AccountStatementReport(
    UUID reportId,
    String generatedAt,
    CustomerInfo customer,
    ReportPeriod period,
    List<AccountStatement> accounts,
    StatementSummary summary
) {}

