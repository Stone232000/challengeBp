package com.espiedra.api.customer.service.infrastructure.config.security;

import com.espiedra.api.customer.service.application.ports.output.JwtTokenPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT Authentication Web Filter - Security Layer
 * <p>
 * This filter intercepts HTTP requests in a reactive WebFlux application and validates JWT tokens.
 * If a valid token is present, it extracts user information and sets the security context.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This is part of the INFRASTRUCTURE layer (security configuration).
 * It uses the JwtTokenPort (output port) to validate tokens.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only handles JWT authentication
 * - Dependency Inversion Principle (DIP): Depends on JwtTokenPort abstraction
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Filter Pattern: Intercepts requests for authentication
 * - Strategy Pattern: Uses JwtTokenPort for token validation
 * - Chain of Responsibility: Part of Spring Security filter chain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtTokenPort jwtTokenPort;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        log.debug("Processing request: {} {}", exchange.getRequest().getMethod(), path);

        // Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // If no Authorization header or doesn't start with "Bearer ", continue without authentication
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No JWT token found in request to: {}", path);
            return chain.filter(exchange);
        }

        // Extract token
        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token and set security context (reactive wrapper for blocking JWT operations)
        return Mono.fromCallable(() -> {
                    try {
                        boolean isValid = jwtTokenPort.validateToken(token);
                        if (!isValid) {
                            log.warn("Invalid JWT token for request: {}", path);
                            return AuthenticationResult.failure("Invalid JWT token");
                        }

                        boolean isExpired = jwtTokenPort.isTokenExpired(token);
                        if (isExpired) {
                            log.warn("Expired JWT token for request: {}", path);
                            return AuthenticationResult.failure("JWT token has expired");
                        }

                        // Extract customerId (customer uuid) from token
                        String customerId = jwtTokenPort.extractCustomerId(token).toString();
                        // Extract identification (customer identification) from token
                        String identification = jwtTokenPort.extractIdentification(token);
                        log.debug("Valid JWT token for user: {} , customerId: {}", identification, customerId);

                        // Create authentication object with USER role
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                identification,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                        return AuthenticationResult.success(authentication);

                    } catch (Exception e) {
                        log.error("Error validating JWT token: {}", e.getMessage());
                        return AuthenticationResult.failure("Error validating JWT token: " + e.getMessage());
                    }
                })
                .flatMap(result -> {
                    if (!result.isSuccess()) {
                        // Token invalid or error occurred, return 401 UNAUTHORIZED
                        // Delegate to JwtAuthenticationEntryPoint for consistent error response
                        return authenticationEntryPoint.sendUnauthorizedResponse(exchange, result.getErrorMessage());
                    }

                    // Set security context and continue
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(result.getAuthentication()));
                });
    }
}
