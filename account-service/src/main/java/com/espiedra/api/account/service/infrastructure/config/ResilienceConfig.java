package com.espiedra.api.account.service.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * INFRASTRUCTURE CONFIGURATION: ResilienceConfig
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Circuit Breaker Pattern: Prevents cascading failures
 * - Retry Pattern: Handles transient failures
 * - Timeout Pattern: Prevents indefinite waiting
 * - Configuration Pattern: Centralized resilience configuration
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles resilience configuration
 * - Open/Closed: Open for new resilience configurations
 * - Dependency Inversion: Provides resilience abstractions
 * <p>
 * RESILIENCE4J OVERVIEW:
 * ======================
 * Lightweight fault tolerance library for Java:
 * <p>
 * COMPONENTS:
 * - CircuitBreaker: Stops calling failing services (fail-fast)
 * - Retry: Retries failed calls with backoff
 * - TimeLimiter: Limits execution time
 * - Bulkhead: Limits concurrent calls
 * - RateLimiter: Limits request rate
 * <p>
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    /**
     * Circuit Breaker Configuration
     */
    @Value("${app.resilience.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;

    @Value("${app.resilience.circuit-breaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${app.resilience.circuit-breaker.wait-duration-in-open-state:20s}")
    private Duration waitDurationInOpenState;

    @Value("${app.resilience.circuit-breaker.permitted-number-of-calls-in-half-open-state:3}")
    private int permittedNumberOfCallsInHalfOpenState;

    @Value("${app.resilience.circuit-breaker.sliding-window-size:20}")
    private int slidingWindowSize;

    @Value("${app.resilience.circuit-breaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    /**
     * Retry Configuration
     */
    @Value("${app.resilience.retry.max-attempts:2}")
    private int retryMaxAttempts;

    @Value("${app.resilience.retry.wait-duration:500ms}")
    private Duration retryWaitDuration;

    /**
     * Timeout Configuration
     */
    @Value("${app.resilience.timeout.enabled:true}")
    private boolean timeoutEnabled;

    @Value("${app.resilience.timeout.duration:5s}")
    private Duration timeoutDuration;

    /**
     * Create CircuitBreaker for Customer Service calls
     * <p>
     * CIRCUIT BREAKER STATES:
     * ========================
     * 1. CLOSED (Normal operation):
     *    - All calls go through
     *    - Tracks failure rate in sliding window
     *    - If failure rate > threshold → OPEN
     * <p>
     * 2. OPEN (Circuit breaker triggered):
     *    - All calls fail immediately (CallNotPermittedException)
     *    - No actual calls to Customer Service
     *    - After wait duration → HALF_OPEN
     * <p>
     * 3. HALF_OPEN (Testing recovery):
     *    - Allows limited number of test calls
     *    - If calls succeed → CLOSED
     *    - If calls fail → OPEN again
     * <p>
     * EXAMPLE SCENARIO:
     * =================
     * - Sliding window: 20 calls
     * - Minimum calls: 5
     * - Failure rate threshold: 50%
     * <p>
     * Call results: S S S F F F F F F F (10 calls, 7 failures = 70%)
     * → Circuit OPENS (70% > 50%)
     * → Next calls fail immediately for 20 seconds
     * → After 20s: Allow 3 test calls
     * → If 3 test calls succeed: Circuit CLOSES
     *
     * @return CircuitBreaker Configured circuit breaker
     */
    @Bean
    public CircuitBreaker customerServiceCircuitBreaker() {
        if (!circuitBreakerEnabled) {
            log.info("Circuit Breaker is DISABLED");
            return CircuitBreaker.ofDefaults("customerService");
        }

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            // Failure rate threshold: If X% of calls fail → OPEN circuit
            .failureRateThreshold(failureRateThreshold)
            // Wait duration in OPEN state before transitioning to HALF_OPEN
            .waitDurationInOpenState(waitDurationInOpenState)
            // Number of test calls allowed in HALF_OPEN state
            .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
            // Sliding window size: Track last N calls
            .slidingWindowSize(slidingWindowSize)
            // Minimum number of calls before calculating failure rate
            .minimumNumberOfCalls(minimumNumberOfCalls)
            // Build configuration
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("customerService", config);

        // Register event listeners for monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                log.warn("Circuit Breaker state transition: {} → {} (failureRate={}%, bufferedCalls={})",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState(),
                    circuitBreaker.getMetrics().getFailureRate(),
                    circuitBreaker.getMetrics().getNumberOfBufferedCalls()
                );
            })
            .onCallNotPermitted(event -> {
                log.warn("Circuit Breaker OPEN: Call not permitted to Customer Service");
            })
            .onError(event -> {
                log.debug("Circuit Breaker error: duration={}ms, error={}",
                    event.getElapsedDuration().toMillis(),
                    event.getThrowable().getMessage()
                );
            });

        log.info("Circuit Breaker configured: name=customerService, " +
            "failureRateThreshold={}%, waitDuration={}, slidingWindow={}, minCalls={}",
            failureRateThreshold, waitDurationInOpenState, slidingWindowSize, minimumNumberOfCalls);

        return circuitBreaker;
    }

    /**
     * Create Retry for Customer Service calls
     * <p>
     * RETRY STRATEGY:
     * ===============
     * - Max attempts: 2 (1 original + 1 retry)
     * - Wait duration: 500ms between retries
     * - Retry on: All exceptions except business exceptions
     * <p>
     * EXAMPLE SCENARIO:
     * =================
     * Request 1 → Network timeout → WAIT 500ms
     * Request 2 (retry) → Success → Return response
     * <p>
     * WITHOUT RETRY:
     * Request 1 → Network timeout → Return error to user
     * <p>
     * IMPORTANT:
     * - Only retries transient errors (network, timeout)
     * - Does NOT retry business errors (CustomerNotFoundException)
     * - Prevents unnecessary retries for permanent failures
     *
     * @return Retry Configured retry
     */
    @Bean
    public Retry customerServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            // Maximum retry attempts (1 original + N retries)
            .maxAttempts(retryMaxAttempts)
            // Wait duration between retries
            .waitDuration(retryWaitDuration)
            // Retry on all exceptions (will be filtered in application layer)
            .retryExceptions(Exception.class)
            // Build configuration
            .build();

        Retry retry = Retry.of("customerService", config);

        // Register event listeners for monitoring
        retry.getEventPublisher()
            .onRetry(event -> {
                log.warn("Retry attempt #{} for Customer Service call: {}",
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage()
                );
            });

        log.info("Retry configured: name=customerService, maxAttempts={}, waitDuration={}",
            retryMaxAttempts, retryWaitDuration);

        return retry;
    }

    /**
     * Create TimeLimiter for Customer Service calls
     * <p>
     * TIMEOUT STRATEGY:
     * =================
     * - Timeout duration: 5 seconds (default)
     * - Cancels operation if exceeds timeout
     * - Works with reactive streams (Mono/Flux)
     * <p>
     * EXAMPLE SCENARIO:
     * =================
     * Request → Customer Service processing slowly
     * → After 5s: Cancel request, return timeout error
     * <p>
     * IMPORTANT:
     * - Prevents indefinite waiting
     * - Complements WebClient timeout
     * - Allows different timeouts per operation
     *
     * @return TimeLimiter Configured time limiter
     */
    @Bean
    public TimeLimiter customerServiceTimeLimiter() {
        if (!timeoutEnabled) {
            log.info("TimeLimiter is DISABLED");
            return TimeLimiter.ofDefaults("customerService");
        }

        TimeLimiterConfig config = TimeLimiterConfig.custom()
            // Timeout duration
            .timeoutDuration(timeoutDuration)
            // Cancel running future on timeout
            .cancelRunningFuture(true)
            // Build configuration
            .build();

        TimeLimiter timeLimiter = TimeLimiter.of("customerService", config);

        log.info("TimeLimiter configured: name=customerService, timeout={}",
            timeoutDuration);

        return timeLimiter;
    }
}
