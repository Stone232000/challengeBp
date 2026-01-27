package com.espiedra.api.customer.service.application.ports.output;

/**
 * Output Port (Password Encoder Interface) - BCrypt Password Encoding
 * <p>
 * HEXAGONAL ARCHITECTURE - SECONDARY PORT:
 * This interface defines the contract for password encoding operations.
 * It is independent of any framework or infrastructure (Spring Security, BCrypt, etc.).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Domain depends on abstraction, not BCrypt implementation
 * - Interface Segregation Principle (ISP): Focused interface for password operations
 * - Single Responsibility Principle (SRP): Only handles password encoding/verification
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port/Adapter Pattern: This is a SECONDARY PORT (output) in hexagonal architecture
 * - Strategy Pattern: Allows swapping password encoding strategies
 */
public interface PasswordEncoderPort {

    /**
     * Encode a raw password using BCrypt.
     *
     * @param rawPassword Plain text password
     * @return Encoded password hash
     */
    String encode(String rawPassword);

    /**
     * Verify if a raw password matches an encoded password.
     *
     * @param rawPassword Plain text password
     * @param encodedPassword BCrypt hash
     * @return true if passwords match, false otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}
