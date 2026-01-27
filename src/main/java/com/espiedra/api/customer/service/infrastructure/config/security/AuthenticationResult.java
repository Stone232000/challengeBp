package com.espiedra.api.customer.service.infrastructure.config.security;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Authentication Result - Security Layer
 * <p>
 * Encapsulates the result of a JWT token validation operation.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Value Object Pattern: Immutable object that represents the result of validation
 * - Result Pattern: Encapsulates success/failure with optional data
 * - Builder Pattern: Uses Lombok @Builder for flexible object construction
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only represents validation result
 * - Immutability: All fields are final for thread safety
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationResult {
    private final boolean success;
    private final String errorMessage;
    private final UsernamePasswordAuthenticationToken authentication;

    /**
     * Factory method for creating a successful result
     *
     * @param authentication Authentication token
     * @return AuthenticationResult with success=true
     */
    public static AuthenticationResult success(UsernamePasswordAuthenticationToken authentication) {
        return AuthenticationResult.builder()
                .success(true)
                .errorMessage(null)
                .authentication(authentication)
                .build();
    }

    /**
     * Factory method for creating a failed result
     *
     * @param errorMessage Error message
     * @return AuthenticationResult with success=false
     */
    public static AuthenticationResult failure(String errorMessage) {
        return AuthenticationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .authentication(null)
                .build();
    }
}
