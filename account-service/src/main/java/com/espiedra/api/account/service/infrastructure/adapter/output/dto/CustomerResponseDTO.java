package com.espiedra.api.account.service.infrastructure.adapter.output.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DESIGN PATTERN:
 * - DTO Pattern: Decouples HTTP response from domain model
 * - Mapper Pattern: Converted to domain Customer model via mapToCustomer()
 * <p>
 * JSON STRUCTURE:
 * {
 *   "customerId": "550e8400-e29b-41d4-a716-446655440000",
 *   "name": "John Doe",
 *   "identification": "1234567890",
 *   "state": true
 * }
 * <p>
 * FIELD DESCRIPTIONS:
 * - customerId: UUID of customer in Customer Service
 * - name: Full name of customer
 * - identification: Government ID (DNI, passport, etc.)
 * - state: TRUE (active), FALSE (inactive)
 * <p>
 * USAGE:
 * - Deserialized from HTTP response body by WebClient
 * - Mapped to Customer domain model before returning to caller
 * <p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {
    /**
     * Customer UUID (primary key)
     */
    private UUID customerId;

    /**
     * Customer full name
     */
    private String name;

    /**
     * Customer identification number (DNI, passport, etc.)
     */
    private String identification;

    /**
     * Customer state: TRUE (active), FALSE (inactive)
     */
    private Boolean state;
}
