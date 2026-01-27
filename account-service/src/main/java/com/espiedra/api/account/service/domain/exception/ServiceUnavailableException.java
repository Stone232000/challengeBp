package com.espiedra.api.account.service.domain.exception;

/**
 * DOMAIN EXCEPTION: ServiceUnavailableException
 * <p>
 * Thrown when an external service (Customer Service, etc.) is temporarily unavailable.
 * This occurs when:
 * 1. Circuit Breaker is OPEN (service is down)
 * 2. Timeout errors (service is slow/unresponsive)
 * 3. Network errors (connection refused, etc.)
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception as Domain Concept: Business exception in domain layer
 * - Circuit Breaker Pattern: Fail-fast when service is unavailable
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only represents service unavailability
 * - Open/Closed: Extends DomainException, closed for modification
 * <p>
 * HTTP MAPPING:
 * - Maps to 503 SERVICE UNAVAILABLE in REST layer
 * <p>
 * USAGE:
 * - Thrown when Customer Service is down (Circuit Breaker OPEN)
 * - Thrown when external service times out
 * - Caught by GlobalExceptionHandler to return 503 HTTP status
 * <p>
 * RESILIENCE:
 * - This exception indicates a transient failure
 * - Client should retry later (after circuit breaker closes)
 * - Better than returning 500 (which implies server error)
 * <p>
 */
public class ServiceUnavailableException extends DomainException {

    /**
     * Constructor with custom message
     *
     * @param message Error message describing the unavailability
     */
    public ServiceUnavailableException(String message) {
        super(message, "SERVICE_UNAVAILABLE");
    }

    /**
     * Constructor with service name
     *
     * @param serviceName The name of the unavailable service
     * @param reason Reason for unavailability (e.g., "Circuit Breaker OPEN", "Timeout")
     */
    public ServiceUnavailableException(String serviceName, String reason) {
        super(String.format("%s is temporarily unavailable: %s", serviceName, reason), "SERVICE_UNAVAILABLE");
    }

    /**
     * Constructor with message and cause
     *
     * @param message Error message
     * @param cause Root cause exception
     */
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, "SERVICE_UNAVAILABLE", cause);
    }
}
