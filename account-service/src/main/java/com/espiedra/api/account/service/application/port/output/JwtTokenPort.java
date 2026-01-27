package com.espiedra.api.account.service.application.port.output;

import java.util.UUID;

/**
 * Output Port (JWT Token Interface) - JWT Token Generation and Validation
 * <p>
 * HEXAGONAL ARCHITECTURE - SECONDARY PORT:
 * This interface defines the contract for JWT token operations.
 * It is independent of any framework or infrastructure (JJWT).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Domain depends on abstraction, not JWT library
 * - Interface Segregation Principle (ISP): Focused interface for token operations
 * - Single Responsibility Principle (SRP): Only handles JWT token operations
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: This is a SECONDARY PORT (output) in hexagonal architecture
 * - Strategy Pattern: Allows swapping JWT implementations
 */
public interface JwtTokenPort {

    /**
     * Validate a JWT token.
     *
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Extract customer ID from a JWT token.
     *
     * @param token JWT token string
     * @return Customer UUID
     */
    UUID extractCustomerId(String token);

    /**
     * Extract identification from a JWT token.
     *
     * @param token JWT token string
     * @return Customer identification
     */
    String extractIdentification(String token);

    /**
     * Check if token is expired.
     *
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    boolean isTokenExpired(String token);
}
