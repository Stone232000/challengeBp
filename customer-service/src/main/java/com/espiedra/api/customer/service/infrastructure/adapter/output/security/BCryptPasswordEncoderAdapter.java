package com.espiedra.api.customer.service.infrastructure.adapter.output.security;

import com.espiedra.api.customer.service.application.ports.output.PasswordEncoderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt Password Encoder Adapter - Infrastructure Layer
 * <p>
 * This adapter implements the PasswordEncoderPort (output port) using Spring Security's BCrypt.
 * It provides password encoding and validation functionality.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is a SECONDARY ADAPTER that implements an OUTPUT PORT.
 * It adapts Spring Security's PasswordEncoder to the domain port interface.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Implements port interface from application layer
 * - Single Responsibility Principle (SRP): Only handles password encoding operations
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts Spring Security BCrypt to domain port interface
 * - Strategy Pattern: Allows swapping password encoding strategies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encode(String rawPassword) {
        log.debug("Encoding password with BCrypt");
        String encodedPassword = passwordEncoder.encode(rawPassword);
        log.debug("Password encoded successfully");
        return encodedPassword;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        log.debug("Validating password with BCrypt");
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        log.debug("Password validation result: {}", matches);
        return matches;
    }
}
