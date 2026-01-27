package com.espiedra.api.customer.service.application.dto;

import com.espiedra.api.customer.service.domain.model.Customer;

/**
 * Result object containing created customer and JWT token.
 * <p>
 * DESIGN PATTERN: Result Object Pattern
 * Encapsulates multiple return values in a single object.
 */
public record CustomerCreationResult (Customer customer, String jwtToken){}
