package com.espiedra.api.customer.service.infrastructure.adapter.input.rest.exception;

import com.espiedra.api.customer.service.domain.exception.*;
import com.espiedra.api.customer.service.infrastructure.input.adapter.rest.customer.service.models.ErrorResponse;
import com.espiedra.api.customer.service.infrastructure.input.adapter.rest.customer.service.models.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global Exception Handler - REST Layer
 * <p>
 * Handles all exceptions thrown by the application and converts them
 * to the ErrorResponse format defined in the OpenAPI contract.
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Only handles exception to HTTP response conversion
 * - Open/Closed Principle (OCP): Easy to add new exception handlers without modifying existing ones
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Exception Handler Pattern: Centralized exception handling
 * - Adapter Pattern: Adapts domain exceptions to REST error responses
 * <p>
 * RestControllerAdvice:
 * - @RestControllerAdvice makes this a global exception handler for all controllers
 * - @ExceptionHandler methods automatically catch exceptions from controllers
 * - Converts domain exceptions to ErrorResponse (OpenAPI contract model)
 * - Returns proper HTTP status codes
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle CustomerNotFoundException (404 Not Found).
     *
     * @param ex CustomerNotFoundException
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCustomerNotFoundException(
            CustomerNotFoundException ex,
            ServerWebExchange exchange
    ) {
        log.error("Customer not found: {}", ex.getMessage());

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse));
    }

    /**
     * Handle CustomerAlreadyExistsException (409 Conflict).
     *
     * @param ex CustomerAlreadyExistsException
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCustomerAlreadyExistsException(
            CustomerAlreadyExistsException ex,
            ServerWebExchange exchange
    ) {
        log.error("Customer already exists: {}", ex.getMessage());

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse));
    }

    /**
     * Handle InvalidPasswordException (400 Bad Request).
     *
     * @param ex InvalidPasswordException
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidPasswordException(
            InvalidPasswordException ex,
            ServerWebExchange exchange
    ) {
        log.error("Invalid password: {}", ex.getMessage());

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

    /**
     * Handle ServerWebInputException (Missing required headers/parameters)
     * Returns 400 BAD REQUEST
     *
     * Common scenarios:
     * - Missing required headers (X-Request-Id, X-Correlation-Id)
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
     * Handle CustomerInactiveException (422 Unprocessable Entity).
     *
     * @param ex CustomerInactiveException
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(CustomerInactiveException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCustomerInactiveException(
            CustomerInactiveException ex,
            ServerWebExchange exchange
    ) {
        log.error("Customer inactive: {}", ex.getMessage());

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse));
    }

    /**
     * Handle OptimisticLockException (409 Conflict).
     *
     * @param ex OptimisticLockException
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(OptimisticLockException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleOptimisticLockException(
            OptimisticLockException ex,
            ServerWebExchange exchange
    ) {
        log.error("Optimistic lock conflict: {}", ex.getMessage());

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse));
    }

    /**
     * Handle Bean Validation Exceptions (400 Bad Request).
     * <p>
     * This handles validation errors from OpenAPI generated models.
     *
     * @param ex WebExchangeBindException
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            WebExchangeBindException ex,
            ServerWebExchange exchange
    ) {
        log.error("Validation error: {}", ex.getMessage());

        // Convert field errors to ValidationError list
        List<ValidationError> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(convertToValidationError(fieldError));
        }

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                exchange.getRequest().getPath().value()
        );
        errorResponse.setErrors(validationErrors);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

    /**
     * Handle Generic Domain Exceptions (500 Internal Server Error).
     *
     * @param ex DomainException
     * @param exchange ServerWebExchange for request context
     * @return Mono<ResponseEntity<ErrorResponse>>
     */
    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDomainException(
            DomainException ex,
            ServerWebExchange exchange
    ) {
        log.error("Domain exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
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

    /**
     * Convert FieldError to ValidationError (OpenAPI model).
     *
     * @param fieldError Spring's FieldError
     * @return ValidationError from OpenAPI contract
     */
    private ValidationError convertToValidationError(FieldError fieldError) {
        ValidationError validationError = new ValidationError();
        validationError.setField(fieldError.getField());
        validationError.setMessage(fieldError.getDefaultMessage());
        validationError.setRejectedValue(fieldError.getRejectedValue());
        return validationError;
    }
}
