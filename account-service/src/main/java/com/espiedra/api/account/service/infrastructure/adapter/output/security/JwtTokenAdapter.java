package com.espiedra.api.account.service.infrastructure.adapter.output.security;

import com.espiedra.api.account.service.application.port.output.JwtTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token Adapter - Infrastructure Layer
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is a SECONDARY ADAPTER that implements an OUTPUT PORT.
 * It adapts JJWT library to the domain port interface.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Dependency Inversion Principle (DIP): Implements port interface from application layer
 * - Single Responsibility Principle (SRP): Only handles JWT token operations
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts JJWT library to domain port interface
 * - Strategy Pattern: Allows swapping JWT implementations
 */
@Slf4j
@Component
public class JwtTokenAdapter implements JwtTokenPort {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    @Value("${app.jwt.audience}")
    private String jwtAudience;

    /**
     * Get signing key from JWT secret.
     *
     * @return SecretKey for signing tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            log.debug("Validating JWT token");

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            log.debug("JWT token is valid");
            return true;

        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UUID extractCustomerId(String token) {
        log.debug("Extracting customer ID from JWT token");

        Claims claims = extractAllClaims(token);
        String customerIdStr = claims.get("customerId", String.class);

        UUID customerId = UUID.fromString(customerIdStr);
        log.debug("Extracted customer ID: {}", customerId);

        return customerId;
    }

    @Override
    public String extractIdentification(String token) {
        log.debug("Extracting identification from JWT token");

        Claims claims = extractAllClaims(token);
        String identification = claims.get("identification", String.class);

        log.debug("Extracted identification: {}", identification);
        return identification;
    }

    @Override
    public boolean isTokenExpired(String token) {
        log.debug("Checking if JWT token is expired");

        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            boolean isExpired = expiration.before(new Date());

            log.debug("Token expired: {}", isExpired);
            return isExpired;

        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Extract all claims from JWT token.
     *
     * @param token JWT token
     * @return Claims object
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
