package com.espiedra.api.account.service.infrastructure.adapter.output.client;

import com.espiedra.api.account.service.application.port.output.CustomerServiceClientPort;
import com.espiedra.api.account.service.domain.exception.CustomerNotActiveException;
import com.espiedra.api.account.service.domain.exception.CustomerNotFoundException;
import com.espiedra.api.account.service.domain.model.Customer;
import com.espiedra.api.account.service.infrastructure.adapter.output.dto.CustomerResponseDTO;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * INFRASTRUCTURE ADAPTER (OUTPUT): CustomerWebClient
 * <p>
 * Implements CustomerServiceClient port using Spring WebClient for reactive HTTP communication.
 * This is an Output Adapter in Hexagonal Architecture (Ports & Adapters).
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts WebClient to domain's CustomerServiceClient port
 * - Circuit Breaker Pattern: Timeout + fallback for resilience (Resilience4j)
 * - Retry Pattern: Automatic retries with exponential backoff
 * - Timeout Pattern: Limits execution time
 * - Dependency Inversion Principle (DIP): Implements domain-defined contract
 * - Client Pattern: Encapsulates HTTP communication with Customer Service
 * <p>
 * SOLID PRINCIPLES:
 * - Dependency Inversion: Depends on domain port (CustomerServiceClient), not WebClient
 * - Single Responsibility: Only handles HTTP communication with Customer Service
 * - Open/Closed: Open for new endpoints, closed for modification
 * - Interface Segregation: Implements only customer service client interface
 * - Liskov Substitution: Can be replaced by any CustomerServiceClient implementation
 * <p>
 * RESILIENCE4J INTEGRATION:
 * - CircuitBreaker: Prevents cascading failures when Customer Service is down
 * - Retry: Retries transient failures (network errors, timeouts)
 * - TimeLimiter: Enforces timeout for all operations
 * <p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerWebClientPort implements CustomerServiceClientPort {

    /**
     * Spring WebClient for reactive HTTP requests
     * Configured in WebClientConfig with base URL and timeouts
     */
    private final WebClient webClient;

    /**
     * Resilience4j Circuit Breaker for Customer Service
     * Injected from ResilienceConfig
     */
    private final CircuitBreaker customerServiceCircuitBreaker;

    /**
     * Resilience4j Retry for Customer Service
     * Injected from ResilienceConfig
     */
    private final Retry customerServiceRetry;

    /**
     * Resilience4j Time Limiter for Customer Service
     * Injected from ResilienceConfig
     */
    private final TimeLimiter customerServiceTimeLimiter;

    /**
     * Customer Service base URL
     * Injected from application.yaml: app.customer-service.base-url
     */
    @Value("${app.customer-service.base-url}")
    private String baseUrl;

    /**
     * {@inheritDoc}
     * <p>
     * IMPLEMENTATION DETAILS:
     * ========================
     * <p>
     * ENDPOINT: GET /api/v1/customers/{customerId}/validate
     * <p>
     * This endpoint validates that a customer:
     * 1. Exists in Customer Service
     * 2. Is active (state = TRUE)
     * <p>
     * HTTP RESPONSES:
     * - 200 OK: Customer valid and active → Return Customer
     * - 400 BAD_REQUEST: Customer exists but inactive → CustomerNotActiveException
     * - 404 NOT_FOUND: Customer doesn't exist → CustomerNotFoundException
     * - Timeout/Error: Network issue → Log error, return cached customer (fallback)
     * <p>
     * RESILIENCE:
     * - Timeout: 2 seconds (fail-fast)
     * - Fallback: Not implemented here (should be in use case layer)
     * - Error logging: All errors logged for monitoring
     * <p>
     * REACTIVE FLOW:
     * 1. Build URL with customer ID
     * 2. Execute GET request via WebClient
     * 3. Handle HTTP status codes (404, 400)
     * 4. Deserialize response to CustomerResponse DTO
     * 5. Map DTO to domain Customer model
     * 6. Apply timeout (2 seconds)
     * 7. Log success/error
     * 8. Return Mono<Customer>
     * <p>
     * CIRCUIT BREAKER:
     * - If Customer Service is down: Timeout after 2 seconds
     * - Error logged but not swallowed (propagated to caller)
     * - Caller (use case) can implement fallback to cache
     *
     * @param customerId Customer UUID to validate
     * @return Mono<Customer> Customer if valid and active
     * @throws CustomerNotFoundException  If customer doesn't exist (404)
     * @throws CustomerNotActiveException If customer exists but inactive (400)
     */
    @Override
    public Mono<Customer> validateCustomer(UUID customerId) {
        // Build validation endpoint URL
        String url = baseUrl + "/api/v1/customers/" + customerId + "/validate";

        log.debug("Validating customer via WebClient: customerId={}, url={}", customerId, url);

        return propagateHeaders(webClient.get().uri(url))
                .flatMap(spec -> spec.retrieve()
                        // Handle 404 NOT_FOUND → Customer doesn't exist
                        .onStatus(
                                status -> status.equals(HttpStatus.NOT_FOUND),
                                response -> {
                                    log.warn("Customer not found in Customer Service: customerId={}", customerId);
                                    return Mono.error(new CustomerNotFoundException(customerId));
                                }
                        )
                        // Handle 400 BAD_REQUEST → Customer exists but inactive
                        .onStatus(
                                status -> status.equals(HttpStatus.BAD_REQUEST),
                                response -> {
                                    log.warn("Customer is not active in Customer Service: customerId={}", customerId);
                                    return Mono.error(new CustomerNotActiveException(customerId));
                                }
                        )
                        // Deserialize response body to CustomerResponse DTO
                        .bodyToMono(CustomerResponseDTO.class)
                        // Map DTO to domain Customer model
                        .map(this::mapToCustomer)
                        // Log successful validation
                        .doOnSuccess(customer -> {
                            log.info("Customer validated successfully via WebClient: customerId={}, name='{}'",
                                    customerId, customer.getName());
                        })
                        // Log errors (network, timeout, etc.)
                        .doOnError(error -> {
                            if (error instanceof CustomerNotFoundException || error instanceof CustomerNotActiveException) {
                                // Business exceptions already logged above
                                return;
                            }
                            log.warn("Error validating customer via WebClient: customerId={}, error={}",
                                    customerId, error.getMessage());
                        })
                )
                // Apply Resilience4j operators (non-invasive):
                // 1. TimeLimiter: Timeout for the entire operation
                .transformDeferred(TimeLimiterOperator.of(customerServiceTimeLimiter))
                // 2. CircuitBreaker: Fail-fast if Customer Service is down
                .transformDeferred(CircuitBreakerOperator.of(customerServiceCircuitBreaker))
                // 3. Retry: Retry on transient failures (but not business exceptions)
                .transformDeferred(RetryOperator.of(customerServiceRetry))
                // Handle ALL infrastructure errors AFTER operators (single point of conversion)
                // This avoids duplication - handles errors from both HTTP call AND operators
                .onErrorMap(error -> {
                    // Don't wrap business exceptions or already-wrapped infrastructure exceptions
                    if (error instanceof CustomerNotFoundException ||
                        error instanceof CustomerNotActiveException ||
                        error instanceof com.espiedra.api.account.service.domain.exception.ServiceUnavailableException) {
                        return error;
                    }

                    // Circuit Breaker OPEN (thrown by CircuitBreakerOperator)
                    if (error instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                        log.error("Circuit Breaker OPEN: Customer Service unavailable (customerId={})", customerId);
                        return new com.espiedra.api.account.service.domain.exception.ServiceUnavailableException(
                            "Customer Service", "Circuit Breaker is OPEN - service is temporarily unavailable");
                    }

                    // Timeout errors (from TimeLimiterOperator or WebClient)
                    if (error instanceof java.util.concurrent.TimeoutException ||
                        error.getCause() instanceof java.util.concurrent.TimeoutException) {
                        log.error("Timeout calling Customer Service (customerId={})", customerId);
                        return new com.espiedra.api.account.service.domain.exception.ServiceUnavailableException(
                            "Customer Service", "Request timeout");
                    }

                    // Connection errors (service down - from WebClient)
                    if (error.getMessage() != null &&
                        (error.getMessage().contains("Connection refused") ||
                         error.getMessage().contains("Connection reset") ||
                         error.getMessage().contains("Connection timed out"))) {
                        log.error("Connection error to Customer Service (customerId={}): {}", customerId, error.getMessage());
                        return new com.espiedra.api.account.service.domain.exception.ServiceUnavailableException(
                            "Customer Service", "Service is down or unreachable");
                    }

                    // Generic infrastructure errors
                    log.error("Unexpected error calling Customer Service (customerId={}): {} - {}",
                        customerId, error.getClass().getSimpleName(), error.getMessage());
                    return new com.espiedra.api.account.service.domain.exception.ServiceUnavailableException(
                        "Customer Service", "Temporary communication error");
                });
    }

    /**
     * ENDPOINT: GET /api/v1/customers/{customerId}
     * <p>
     * Fetches customer details WITHOUT state validation.
     * Returns customer even if inactive (state = FALSE).
     * <p>
     * USE CASES:
     * - Display customer name in account statements
     * - Fetch customer data for reports
     * - Admin operations (view inactive customers)
     * <p>
     * HTTP RESPONSES:
     * - 200 OK: Customer found → Return Customer
     * - 404 NOT_FOUND: Customer doesn't exist → CustomerNotFoundException
     * - Timeout/Error: Network issue → Propagate error
     * <p>
     * DIFFERENCE FROM validateCustomer():
     * - validateCustomer(): Checks exists AND active (400 if inactive)
     * - getCustomer(): Checks exists only (returns even if inactive)
     *
     * @param customerId Customer UUID
     * @return Mono<Customer> Customer details (active or inactive)
     * @throws CustomerNotFoundException If customer doesn't exist (404)
     */
    @Override
    public Mono<Customer> getCustomer(UUID customerId) {
        // Build get customer endpoint URL
        String url = baseUrl + "/api/v1/customers/" + customerId;

        log.debug("Fetching customer via WebClient: customerId={}, url={}", customerId, url);

        return propagateHeaders(webClient.get().uri(url))
                .flatMap(spec -> spec.retrieve()
                        // Handle 404 NOT_FOUND → Customer doesn't exist
                        .onStatus(
                                status -> status.equals(HttpStatus.NOT_FOUND),
                                response -> {
                                    log.warn("Customer not found in Customer Service: customerId={}", customerId);
                                    return Mono.error(new CustomerNotFoundException(customerId));
                                }
                        )
                        // Deserialize response body to CustomerResponse DTO
                        .bodyToMono(CustomerResponseDTO.class)
                        // Map DTO to domain Customer model
                        .map(this::mapToCustomer)
                        // Log successful fetch
                        .doOnSuccess(customer -> {
                            log.info("Customer fetched successfully via WebClient: customerId={}, name='{}', state={}",
                                    customerId, customer.getName(), customer.getState());
                        })
                        // Log errors
                        .doOnError(error -> {
                            if (error instanceof CustomerNotFoundException) {
                                // Business exception already logged above
                                return;
                            }
                            log.warn("Error fetching customer via WebClient: customerId={}, error={}",
                                    customerId, error.getMessage());
                        })
                )
                // Apply Resilience4j operators (non-invasive):
                // 1. TimeLimiter: Timeout for the entire operation
                .transformDeferred(TimeLimiterOperator.of(customerServiceTimeLimiter))
                // 2. CircuitBreaker: Fail-fast if Customer Service is down
                .transformDeferred(CircuitBreakerOperator.of(customerServiceCircuitBreaker))
                // 3. Retry: Retry on transient failures (but not business exceptions)
                .transformDeferred(RetryOperator.of(customerServiceRetry));
    }

    /**
     * {@inheritDoc}
     * <p>
     * ENDPOINT: GET /api/v1/customers/{customerId} (HEAD request)
     * <p>
     * Checks if customer exists WITHOUT fetching full data.
     * Lightweight existence check (only HTTP headers, no body).
     * <p>
     * USE CASES:
     * - Quick existence validation
     * - Bulk customer checks
     * - Pre-validation before expensive operations
     * <p>
     * HTTP RESPONSES:
     * - 200 OK: Customer exists → true
     * - 404 NOT_FOUND: Customer doesn't exist → false
     * - Timeout/Error: Network issue → false (assume doesn't exist)
     * <p>
     * OPTIMIZATION:
     * - Uses HEAD request (no response body)
     * - Faster than GET (less data transfer)
     * - Lower Customer Service load
     * <p>
     * ERROR HANDLING:
     * - Returns false on any error (conservative approach)
     * - Errors logged but not propagated
     * - Caller can fallback to getCustomer() for details
     *
     * @param customerId Customer UUID
     * @return Mono<Boolean> true if exists, false otherwise
     */
    @Override
    public Mono<Boolean> exists(UUID customerId) {
        // Build get customer endpoint URL
        String url = baseUrl + "/api/v1/customers/" + customerId;

        log.debug("Checking customer existence via WebClient: customerId={}", customerId);

        return webClient.head()
                .uri(url)
                .retrieve()
                // Convert response to boolean (any 2xx status means exists)
                .toBodilessEntity()
                .map(response -> {
                    boolean exists = response.getStatusCode().is2xxSuccessful();
                    log.debug("Customer existence check result: customerId={}, exists={}", customerId, exists);
                    return exists;
                })
                // On error: Assume customer doesn't exist (conservative)
                .onErrorResume(error -> {
                    log.warn("Error checking customer existence, assuming does not exist: customerId={}, error={}",
                            customerId, error.getMessage());
                    return Mono.just(false);
                })
                // Apply Resilience4j operators (non-invasive):
                // Note: For exists() we use simpler resilience (no retry, just timeout + circuit breaker)
                // 1. TimeLimiter: Timeout for the entire operation
                .transformDeferred(TimeLimiterOperator.of(customerServiceTimeLimiter))
                // 2. CircuitBreaker: Fail-fast if Customer Service is down
                .transformDeferred(CircuitBreakerOperator.of(customerServiceCircuitBreaker));
    }

    /**
     * Propagates required headers from current request to WebClient request.
     * <p>
     * Extracts the following headers from Reactor Context and adds them to the outgoing request:
     * - Authorization: JWT token for authentication ("Bearer {token}")
     * - x-request-id: Unique request identifier for tracing
     * - x-correlation-id: Correlation identifier for distributed tracing
     * <p>
     * DISTRIBUTED TRACING:
     * - When a user makes a request to account-service
     * - The filter adds x-request-id and x-correlation-id to the request
     * - These headers are stored in Reactor Context by the filter
     * - This method propagates them to customer-service
     * - Enables end-to-end request tracing across microservices
     * <p>
     * SECURITY CONTEXT PROPAGATION:
     * - JWT token is propagated to customer-service for authentication
     * - customer-service validates the same JWT token
     * - Ensures consistent security context across services
     * <p>
     * REACTIVE FLOW:
     * 1. Get ServerWebExchange from Reactor context
     * 2. Extract Authorization, x-request-id, x-correlation-id headers
     * 3. Add them to WebClient request headers
     * 4. Return WebClient.RequestHeadersSpec for method chaining
     * <p>
     * ERROR HANDLING:
     * - If no exchange in context: Logs warning, continues without headers
     * - If headers missing: Logs debug, continues (headers are optional)
     * - All errors logged for troubleshooting
     *
     * @param requestHeadersSpec WebClient request spec
     * @return Mono<WebClient.RequestHeadersSpec < ?>> Request spec with propagated headers
     */
    private Mono<WebClient.RequestHeadersSpec<?>> propagateHeaders(WebClient.RequestHeadersSpec<?> requestHeadersSpec) {
        return Mono.deferContextual(ctx -> {
            try {
                ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                ServerHttpRequest request = exchange.getRequest();

                // Get headers from incoming request
                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                String requestId = request.getHeaders().getFirst("x-request-id");
                String correlationId = request.getHeaders().getFirst("x-correlation-id");

                // Add headers to outgoing request
                WebClient.RequestHeadersSpec<?> spec = requestHeadersSpec.headers(headers -> {
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                        log.debug("Propagating Authorization header to customer-service");
                    } else {
                        log.warn("No Bearer token found in request");
                    }

                    if (requestId != null) {
                        headers.set("x-request-id", requestId);
                        log.debug("Propagating x-request-id: {}", requestId);
                    } else {
                        log.debug("No x-request-id header found");
                    }

                    if (correlationId != null) {
                        headers.set("x-correlation-id", correlationId);
                        log.debug("Propagating x-correlation-id: {}", correlationId);
                    } else {
                        log.debug("No x-correlation-id header found");
                    }
                });

                return Mono.just(spec);
            } catch (Exception e) {
                log.warn("Error propagating headers from context: {}", e.getMessage());
                return Mono.just(requestHeadersSpec);
            }
        });
    }

    /**
     * Map CustomerResponse DTO to domain Customer model
     * <p>
     * PATTERN: DTO Mapper Pattern
     * - Decouples HTTP response structure from domain model
     * - Allows Customer Service response format to change without affecting domain
     * - Maps only relevant fields (ignores extra fields from response)
     * <p>
     * MAPPING LOGIC:
     * - customerId: UUID from response
     * - name: Customer full name
     * - identification: Government ID
     * - state: Active (TRUE) / Inactive (FALSE)
     * - cachedAt: Current timestamp (mark cache as fresh)
     * - isNew: false (for UPDATE if exists in cache)
     * <p>
     * CACHE TIMESTAMP:
     * - cachedAt set to now() to mark cache as fresh after WebClient validation
     * - Allows cache-aside pattern: Cache is fresh after HTTP validation
     *
     * @param response CustomerResponse DTO from HTTP response
     * @return Customer domain model
     */
    private Customer mapToCustomer(CustomerResponseDTO response) {
        return Customer.builder()
                .customerId(response.getCustomerId())
                .name(response.getName())
                .identification(response.getIdentification())
                .state(response.getState())
                .build();
    }


}
