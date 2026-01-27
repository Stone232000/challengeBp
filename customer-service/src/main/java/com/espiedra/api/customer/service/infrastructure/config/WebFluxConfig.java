package com.espiedra.api.customer.service.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

import java.util.UUID;

/**
 * WebFlux Configuration
 * <p>
 * Configures WebFlux filters and utilities.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Filter Pattern: Adds correlation and request IDs to all requests
 * - MDC Pattern: Distributed tracing with Correlation ID and Request ID
 */
@Slf4j
@Configuration
public class WebFluxConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    /**
     * WebFilter for Correlation ID and Request ID tracking.
     * <p>
     * This filter extracts or generates correlation and request IDs for distributed tracing.
     * IDs are added to MDC (Mapped Diagnostic Context) for structured logging.
     * <p>
     * DESIGN PATTERN: Filter Pattern for cross-cutting concerns
     *
     * @return WebFilter
     */
    @Bean
    public WebFilter correlationIdFilter() {
        return (exchange, chain) -> {
            // Extract or generate Correlation ID
            String correlationId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(CORRELATION_ID_HEADER);

            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Generated new Correlation ID: {}", correlationId);
            }

            // Extract or generate Request ID
            String requestId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(REQUEST_ID_HEADER);

            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
                log.debug("Generated new Request ID: {}", requestId);
            }

            // Add to response headers
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
            exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

            // Store in context for reactive chain
            final String finalCorrelationId = correlationId;
            final String finalRequestId = requestId;

            return chain.filter(exchange)
                    .contextWrite(ctx -> ctx
                            .put(CORRELATION_ID_MDC_KEY, finalCorrelationId)
                            .put(REQUEST_ID_MDC_KEY, finalRequestId)
                    )
                    .doOnSubscribe(subscription -> {
                        // Add to MDC for logging
                        MDC.put(CORRELATION_ID_MDC_KEY, finalCorrelationId);
                        MDC.put(REQUEST_ID_MDC_KEY, finalRequestId);
                    })
                    .doFinally(signalType -> {
                        // Clean up MDC
                        MDC.remove(CORRELATION_ID_MDC_KEY);
                        MDC.remove(REQUEST_ID_MDC_KEY);
                    });
        };
    }

    /**
     * WebFilter for request logging.
     * <p>
     * Logs incoming requests with method, path, and tracing IDs.
     *
     * @return WebFilter
     */
    @Bean
    public WebFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getPath().value();
            String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
            String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);

            log.info("Incoming request: {} {} [correlationId={}, requestId={}]",
                    method, path, correlationId, requestId);

            long startTime = System.currentTimeMillis();

            return chain.filter(exchange)
                    .doFinally(signalType -> {
                        long duration = System.currentTimeMillis() - startTime;
                        int statusCode = exchange.getResponse().getStatusCode() != null ?
                                exchange.getResponse().getStatusCode().value() : 0;

                        log.info("Completed request: {} {} - Status: {} - Duration: {}ms [correlationId={}, requestId={}]",
                                method, path, statusCode, duration, correlationId, requestId);
                    });
        };
    }
}
