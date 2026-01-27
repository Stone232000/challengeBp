package com.espiedra.api.account.service.infrastructure.config;

import com.espiedra.api.account.service.infrastructure.config.security.JwtAuthenticationEntryPoint;
import com.espiedra.api.account.service.infrastructure.config.security.JwtAuthenticationWebFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration - Reactive WebFlux
 * <p>
 * Configures Spring Security for reactive web applications.
 * - BCrypt password encoding
 * - CORS configuration
 * - JWT authentication with custom filter
 * - Public endpoints: /health, /actuator/**, /api-docs/**, /swagger-ui/**
 * - Protected endpoints: All /api/v1/accounts/** routes (requires JWT)
 * <p>
 * ACCOUNT SERVICE SPECIFIC:
 * - No public registration endpoint (unlike customer-service)
 * - All account operations require valid JWT token
 * - Only health checks and documentation are public
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only configures security
 * - Open/Closed Principle (OCP): Extended with JWT filter
 * - Dependency Inversion Principle (DIP): Depends on JwtAuthenticationWebFilter abstraction
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Factory Pattern: Creates security beans
 * - Configuration Pattern: Externalizes security settings
 * - Chain of Responsibility: Security filter chain with JWT filter
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    /**
     * Configure BCrypt password encoder.
     * <p>
     * SECURITY BEST PRACTICE:
     * - BCrypt is a secure hashing algorithm resistant to rainbow table attacks
     * - Strength of 10 is a good balance between security and performance
     *
     * @return PasswordEncoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Configure security filter chain.
     * <p>
     * SECURITY CONFIGURATION:
     * - Disable CSRF for stateless API (JWT-based)
     * - Configure CORS for inter-microservice communication
     * - Conditional JWT authentication based on app.security.enabled property
     * <p>
     * WHEN SECURITY IS ENABLED (app.security.enabled=true):
     * - Add JWT authentication filter
     * - Configure public endpoints (no authentication required):
     *   - /health (health check)
     *   - /actuator/** (Spring Boot actuator endpoints)
     *   - /api-docs/**, /swagger-ui/** (API documentation)
     * - Configure protected endpoints (JWT required):
     *   - ALL /api/v1/accounts/** endpoints (requires authentication)
     *   - No public registration endpoint (account-service specific)
     * <p>
     * WHEN SECURITY IS DISABLED (app.security.enabled=false):
     * - No JWT filter applied
     * - All endpoints are accessible without authentication
     * - Useful for local development and testing
     *
     * @param http ServerHttpSecurity
     * @return SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        ServerHttpSecurity httpSecurity = http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        if (securityEnabled) {
            // Security ENABLED - Apply JWT authentication
            httpSecurity
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    )
                    .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                    .authorizeExchange(exchanges -> exchanges
                            // Public endpoints (no authentication required)
                            .pathMatchers("/health").permitAll()
                            .pathMatchers("/actuator/**").permitAll()
                            .pathMatchers("/api-docs/**").permitAll()
                            .pathMatchers("/swagger-ui/**").permitAll()
                            .pathMatchers("/swagger-ui.html").permitAll()
                            .pathMatchers("/webjars/**").permitAll()

                            // Protected endpoints (JWT authentication required)
                            // ALL account operations require authentication (no public registration)
                            .pathMatchers("/api/v1/accounts/**").authenticated()
                            .pathMatchers("/api/v1/accounts").authenticated()

                            // All other endpoints require authentication
                            .anyExchange().authenticated()
                    );
        } else {
            // Security DISABLED - Allow all requests (for development)
            httpSecurity
                    .authorizeExchange(exchanges -> exchanges
                            .anyExchange().permitAll()
                    );
        }

        return httpSecurity.build();
    }

    /**
     * Configure CORS (Cross-Origin Resource Sharing).
     * <p>
     * Allows frontend applications and inter-microservice communication.
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins for development
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers
        configuration.addAllowedHeader("*");

        // Expose common headers
        configuration.setExposedHeaders(Arrays.asList(
                "X-Request-ID",
                "X-Correlation-ID",
                "Authorization",
                "Content-Type"
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
