package com.espiedra.api.account.service.application.dto;

/**
 * DTO: Report Period
 * <p>
 * Date range for report generation.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer date range information
 * <p>
 */
public record ReportPeriod(
    String startDate,
    String endDate
) {}
