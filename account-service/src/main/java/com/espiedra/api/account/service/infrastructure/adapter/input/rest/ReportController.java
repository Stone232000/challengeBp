package com.espiedra.api.account.service.infrastructure.adapter.input.rest;

import com.espiedra.api.account.service.application.port.input.GenerateAccountStatementUseCase;
import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.application.port.output.MovementRepositoryPort;
import com.espiedra.api.account.service.domain.model.Movement;
import com.espiedra.api.account.service.domain.model.MovementType;
import com.espiedra.api.account.service.infrastructure.adapter.input.rest.mapper.ReportApiMapper;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.api.ReportsApi;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST CONTROLLER: ReportController
 * <p>
 * Primary/Driving Adapter in Hexagonal Architecture.
 * Handles HTTP requests for report generation operations.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts HTTP/REST to domain use cases
 * - Anti-Corruption Layer: Maps between OpenAPI models and Application DTOs
 * - Controller Pattern: Handles HTTP request/response
 * - Query Pattern: Read-only operations for reporting
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles HTTP/REST concerns for reports
 * - Dependency Inversion: Depends on use case abstractions
 * - Interface Segregation: Implements OpenAPI-generated interface
 * <p>
 * FEATURES:
 * - Reactive endpoints (returns Mono)
 * - OpenAPI Contract-First approach
 * - Date range validation
 * - Comprehensive account statements
 * - Customer-centric reporting
 * - MapStruct for DTO mapping
 * <p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController implements ReportsApi {

    private final GenerateAccountStatementUseCase generateAccountStatementUseCase;
    private final MovementRepositoryPort movementRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;

    // Mapper for OpenAPI <-> Application DTO conversion
    private final ReportApiMapper reportApiMapper;

    /**
     * GET /reports/account-statement/{customerId}
     * Generate comprehensive account statement for a customer
     * <p>
     * Implementation of OpenAPI-generated interface method.
     * <p>
     * BUSINESS FLOW:
     * 1. Validate date range
     * 2. Validate customer exists (via cache or WebClient)
     * 3. Retrieve all customer accounts
     * 4. For each account, get movements in date range
     * 5. Calculate initial/final balances
     * 6. Generate summary statistics
     * 7. Map AccountStatementReport (DTO) to AccountStatementResponse (OpenAPI)
     * 8. Return complete report
     * <p>
     * REPORT CONTENTS:
     * - Customer information (name, identification)
     * - List of accounts with balances
     * - Movement details per account
     * - Summary: total credits, debits, net change
     *
     * @param customerId Customer ID
     * @param startDate Start date (ISO 8601 format: yyyy-MM-dd)
     * @param endDate End date (ISO 8601 format: yyyy-MM-dd)
     * @param xRequestId ID único de la petición para trazabilidad
     * @param xCorrelationId ID de correlación para trazar operaciones entre servicios
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<AccountStatementResponse>> 200 with report or 404
     */
    @Override
    public Mono<ResponseEntity<AccountStatementResponse>> getAccountStatementReport(
            UUID customerId,
            LocalDate startDate,
            LocalDate endDate,
            UUID xRequestId,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.info("Generating account statement: customer={}, period={} to {} [requestId={}, correlationId={}]",
                customerId, startDate, endDate, xRequestId, xCorrelationId);

        // Validate date range
        if (startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate={} is after endDate={} [requestId={}]",
                    startDate, endDate, xRequestId);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return generateAccountStatementUseCase.generateAccountStatement(customerId, startDate, endDate)
                .map(report -> {
                    // Map Application DTO to OpenAPI response
                    var response = reportApiMapper.toResponse(report);
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Account statement generated successfully: customer={}, reportId={} [requestId={}]",
                                customerId, response.getBody().getReportId(), xRequestId);
                    }
                })
                .doOnError(error -> log.error("Error generating account statement [requestId={}]: {}",
                        xRequestId, error.getMessage()))
                // IMPORTANT: Propagate ServerWebExchange context for WebClient JWT propagation
                .contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange));
    }

    /**
     * GET /reports/movements-summary
     * Generate summary statistics for movements
     *
     * Implementation of OpenAPI-generated interface method.
     * <p>
     * QUERY PARAMS:
     * - startDate: Start date (required)
     * - endDate: End date (required)
     * - accountNumber: Filter by account (optional)
     * - customerId: Filter by customer (optional)
     * <p>
     * SUMMARY INCLUDES:
     * - Total credits and debits
     * - Count of transactions
     * - Average transaction amount
     * - Largest and smallest transactions
     *
     * @param startDate Start date
     * @param endDate End date
     * @param xRequestId ID único de la petición para trazabilidad
     * @param accountNumber Optional account filter
     * @param customerId Optional customer filter
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<MovementsSummaryResponse>> 200 with summary
     */
    @Override
    public Mono<ResponseEntity<MovementsSummaryResponse>> getMovementsSummary(
            LocalDate startDate,
            LocalDate endDate,
            UUID xRequestId,
            Long accountNumber,
            UUID customerId,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.info("Generating movements summary: account={}, customer={}, period={} to {} [requestId={}]",
                accountNumber, customerId, startDate, endDate, xRequestId);

        // Validate that at least one filter is provided
        if (accountNumber == null && customerId == null) {
            log.warn("No filter provided (accountNumber or customerId required) [requestId={}]", xRequestId);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Validate date range
        if (startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate={} is after endDate={} [requestId={}]",
                    startDate, endDate, xRequestId);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Convert LocalDate to LocalDateTime for repository query
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Get movements based on filters
        Flux<Movement> movementsFlux;

        if (accountNumber != null) {
            // Filter by account number
            log.debug("Fetching movements for account: {} [requestId={}]", accountNumber, xRequestId);
            movementsFlux = movementRepositoryPort.findByAccountNumberAndDateRange(accountNumber, start, end);
        } else {
            // Filter by customer: Get all accounts for customer, then get movements for each account
            log.debug("Fetching movements for customer: {} [requestId={}]", customerId, xRequestId);
            movementsFlux = accountRepositoryPort.findByCustomerId(customerId)
                    .flatMap(account -> movementRepositoryPort.findByAccountNumberAndDateRange(
                            account.getAccountNumber(), start, end));
        }

        // Calculate summary statistics
        return movementsFlux
                .collectList()
                .map(movements -> {
                    if (movements.isEmpty()) {
                        log.debug("No movements found for the given filters [requestId={}]", xRequestId);
                        // Return empty summary
                        return new MovementsSummaryResponse()
                                .period(new ReportPeriod()
                                        .startDate(startDate)
                                        .endDate(endDate))
                                .totalCredits(0.0)
                                .totalDebits(0.0)
                                .creditCount(0)
                                .debitCount(0)
                                .averageTransactionAmount(0.0)
                                .largestTransaction(0.0)
                                .smallestTransaction(0.0);
                    }

                    // Calculate statistics
                    BigDecimal totalCredits = movements.stream()
                            .filter(m -> m.getMovementType() == MovementType.CREDITO)
                            .map(Movement::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalDebits = movements.stream()
                            .filter(m -> m.getMovementType() == MovementType.DEBITO)
                            .map(Movement::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long creditCount = movements.stream()
                            .filter(m -> m.getMovementType() == MovementType.CREDITO)
                            .count();

                    long debitCount = movements.stream()
                            .filter(m -> m.getMovementType() == MovementType.DEBITO)
                            .count();

                    BigDecimal totalAmount = movements.stream()
                            .map(Movement::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    double averageAmount = movements.isEmpty() ? 0.0
                            : totalAmount.divide(BigDecimal.valueOf(movements.size()), 2, RoundingMode.HALF_UP).doubleValue();

                    BigDecimal largestTransaction = movements.stream()
                            .map(Movement::getAmount)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

                    BigDecimal smallestTransaction = movements.stream()
                            .map(Movement::getAmount)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

                    MovementsSummaryResponse summaryResponse = new MovementsSummaryResponse()
                            .period(new ReportPeriod()
                                    .startDate(startDate)
                                    .endDate(endDate))
                            .totalCredits(totalCredits.doubleValue())
                            .totalDebits(totalDebits.doubleValue())
                            .creditCount((int) creditCount)
                            .debitCount((int) debitCount)
                            .averageTransactionAmount(averageAmount)
                            .largestTransaction(largestTransaction.doubleValue())
                            .smallestTransaction(smallestTransaction.doubleValue());

                    log.info("Movements summary generated: totalMovements={}, totalCredits={}, totalDebits={} [requestId={}]",
                            movements.size(), totalCredits, totalDebits, xRequestId);

                    return summaryResponse;
                })
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("Error generating movements summary [requestId={}]: {}",
                        xRequestId, error.getMessage()));
    }
}
