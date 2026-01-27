package com.espiedra.api.account.service.application.dto;

import java.util.UUID;

/**
 * DTO: Customer Information
 * <p>
 * Basic customer information for reports.
 * <p>
 * PATTERN: Data Transfer Object (DTO)
 * PURPOSE: Transfer customer data in reports
 * <p>
 */
public record CustomerInfo(
    UUID customerId,
    String name,
    String identification
) {}
