package com.espiedra.api.customer.service.infrastructure.config.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point - Security Layer
 * <p>
 * This component handles authentication failures by returning a custom JSON error response
 * when a user tries to access a protected endpoint without a valid JWT token.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is part of the INFRASTRUCTURE layer (security configuration).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only handles authentication error responses
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Entry Point Pattern: Defines custom behavior for authentication failures
 * - Error Response Pattern: Standardized error response structure
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        log.warn("Authentication failed for request: {} {} - Reason: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage());

        String message = "JWT token is missing or invalid. Please provide a valid Authorization header with Bearer token.";
        return sendUnauthorizedResponse(exchange, message);
    }

    /**
     * Send 401 UNAUTHORIZED response with custom error message.
     * This method can be reused by other security components.
     *
     * @param exchange ServerWebExchange
     * @param errorMessage Custom error message
     * @return Mono<Void>
     */
    public Mono<Void> sendUnauthorizedResponse(ServerWebExchange exchange, String errorMessage) {
        // Set response status and content type
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Create error response body
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", OffsetDateTime.now().toString());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.put("message", errorMessage);
        errorResponse.put("path", exchange.getRequest().getPath().value());

        try {
            // Serialize error response to JSON
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));

        } catch (JsonProcessingException e) {
            log.error("Error serializing authentication error response: {}", e.getMessage());
            return exchange.getResponse().setComplete();
        }
    }
}
