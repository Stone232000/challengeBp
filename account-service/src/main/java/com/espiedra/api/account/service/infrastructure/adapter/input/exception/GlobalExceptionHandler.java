package com.espiedra.api.account.service.infrastructure.adapter.input.exception;

import com.espiedra.api.account.service.domain.exception.*;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GLOBAL EXCEPTION HANDLER
 * <p>
 * Centralized exception handling for all REST controllers.
 * Provides consistent error responses across the API.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception Handler Pattern: Centralized error handling
 * - Adapter Pattern: Adapts domain exceptions to HTTP responses
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles exception mapping
 * - Open/Closed: Easy to add new exception handlers
 * <p>
 * HTTP STATUS CODE MAPPING:
 * - 400 BAD REQUEST: Validation errors, invalid amounts, inactive accounts
 * - 404 NOT FOUND: Account/Customer not found
 * - 409 CONFLICT: Duplicate transactions, optimistic locking failures
 * - 422 UNPROCESSABLE ENTITY: Insufficient balance, business rule violations
 * - 500 INTERNAL SERVER ERROR: Unexpected errors
 * <p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle AccountNotFoundException
     * Returns 404 NOT FOUND
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAccountNotFound(
            AccountNotFoundException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Account not found: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    /**
     * Handle CustomerNotFoundException
     * Returns 404 NOT FOUND
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCustomerNotFound(
            CustomerNotFoundException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Customer not found: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    /**
     * Handle InsufficientBalanceException
     * Returns 422 UNPROCESSABLE ENTITY
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInsufficientBalance(
            InsufficientBalanceException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Insufficient balance: currentBalance={}, requestedAmount={}, overdraftLimit={}",
                ex.getCurrentBalance(), ex.getRequestedAmount(), ex.getOverdraftLimit());

        ErrorResponse error = createErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    /**
     * Handle CustomerNotActiveException
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(CustomerNotActiveException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCustomerNotActive(
            CustomerNotActiveException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Customer not active: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle ServiceUnavailableException
     * Returns 503 SERVICE UNAVAILABLE
     * <p>
     * This exception is thrown when:
     * - Circuit Breaker is OPEN (Customer Service is down)
     * - External service timeout
     * - Network errors when calling external services
     * <p>
     * HTTP 503 indicates a transient failure - client should retry later.
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServiceUnavailable(
            ServiceUnavailableException ex,
            ServerWebExchange exchange
    ) {
        log.error("Service unavailable: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
    }

    /**
     * Handle AccountNotActiveException
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(AccountNotActiveException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAccountNotActive(
            AccountNotActiveException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Account not active: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle DuplicateTransactionException
     * Returns 409 CONFLICT
     */
    @ExceptionHandler(DuplicateTransactionException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateTransaction(
            DuplicateTransactionException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Duplicate transaction: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    /**
     * Handle DuplicateIdempotencyKeyException
     * Returns 409 CONFLICT
     * <p>
     * This exception is thrown when a client retries a request with an idempotency key
     * that has already been used for a different transaction.
     */
    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateIdempotencyKey(
            DuplicateIdempotencyKeyException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Duplicate idempotency key: {} (existing movementId: {})",
                ex.getIdempotencyKey(), ex.getExistingMovementId());

        ErrorResponse error = createErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    /**
     * Handle InvalidAmountException
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidAmountException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidAmount(
            InvalidAmountException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Invalid amount: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle Validation Errors (Spring Validation)
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationErrors(
            WebExchangeBindException ex,
            ServerWebExchange exchange
    ) {
        // Log validation errors
        Map<String, String> validationErrors = new HashMap<>();
        ex.getFieldErrors().forEach(fieldError ->
                validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        log.warn("Validation failed: {}", validationErrors);

        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed: " + validationErrors,
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle IllegalArgumentException
     * Returns 400 BAD REQUEST
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(
            IllegalArgumentException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle IllegalStateException
     * Returns 400 BAD REQUEST
     * <p>
     * Common scenarios:
     * - Cannot delete account with non-zero balance
     * - Invalid state transitions
     */
    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalState(
            IllegalStateException ex,
            ServerWebExchange exchange
    ) {
        log.warn("Illegal state: {}", ex.getMessage());

        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle DataIntegrityViolationException from database triggers/constraints
     * Returns 422 UNPROCESSABLE ENTITY
     * <p>
     * Common scenarios:
     * - Insufficient balance (from database trigger check)
     * - Constraint violations from database rules
     * - Foreign key violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            ServerWebExchange exchange
    ) {
        String message = ex.getMessage();

        // Extract user-friendly message from exception
        // PostgresSQL error format: "executeMany; SQL [...]; <user_message>"
        if (message != null && message.contains("Saldo no disponible")) {
            // Extract the custom error message from the database
            int startIndex = message.lastIndexOf(';');
            if (startIndex != -1) {
                message = message.substring(startIndex + 1).trim();
            }
            log.warn("Insufficient balance (database constraint): {}", message);

            ErrorResponse error = createErrorResponse(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    message,
                    exchange.getRequest().getPath().value()
            );

            return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
        }

        // Generic data integrity violation
        log.error("Data integrity violation: {}", message);

        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Data integrity violation: The operation violates database constraints",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle ServerWebInputException (Missing required headers/parameters)
     * Returns 400 BAD REQUEST
     * <p>
     * Common scenarios:
     * - Missing required headers (X-Request-Id, X-Correlation-Id, Idempotency-Key)
     * - Invalid header format
     * - Missing required query/path parameters
     */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(
            ServerWebInputException ex,
            ServerWebExchange exchange
    ) {
        String message = ex.getMessage();

        // Extract user-friendly message for missing headers
        if (message.contains("Required request header")) {
            // Extract header name from message
            log.warn("Missing required header: {}", message);
            ErrorResponse error = createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Missing required header. Please provide all mandatory headers (X-Request-Id, X-Correlation-Id, etc.)",
                    exchange.getRequest().getPath().value()
            );
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
        }

        // Generic server input validation error
        log.warn("Server web input validation error: {}", message);
        ErrorResponse error = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                !message.isEmpty() ? message : "Invalid request input",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Handle Generic Exceptions (500 Internal Server Error).
     *
     * @param ex Exception
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex,
            ServerWebExchange exchange
    ) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    /**
     * Create ErrorResponse using setters (OpenAPI generated models don't have @Builder).
     *
     * @param status HTTP status
     * @param message Error message
     * @param path Request path
     * @return ErrorResponse
     */
    private ErrorResponse createErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage(message);
        errorResponse.setPath(path);
        errorResponse.setTraceId(UUID.randomUUID());
        return errorResponse;
    }

}
