package com.espiedra.api.account.service.application.port.input;

import com.espiedra.api.account.service.application.dto.AccountStatementReport;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * APPLICATION PORT (INPUT): GenerateAccountStatementUseCase
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Port Pattern (Hexagonal Architecture): Interface defining business capability
 * - Query Pattern: Read-only operation for reporting
 * - Interface Segregation Principle (ISP): Specific interface for one use case
 * <p>
 * SOLID PRINCIPLES:
 * - Interface Segregation: Clients depend only on report generation operation
 * - Dependency Inversion: Domain depends on abstraction, not concrete implementation
 * - Single Responsibility: Only defines report generation contract
 * <p>
 * IMPLEMENTATION:
 * - Implemented by AccountService in application layer
 * - Called by REST controllers (primary adapters)
 * <p>
 * USE CASE:
 * - Generate comprehensive account statement for a customer
 * - Includes all accounts, movements, and summary
 * - Used for monthly statements, auditing, customer requests
 * <p>
 */
public interface GenerateAccountStatementUseCase {

    /**
     * Generate account statement report for a customer
     * <p>
     * BUSINESS FLOW:
     * 1. Validate customer exists (via WebClient)
     * 2. Retrieve all customer accounts
     * 3. For each account, get movements in date range
     * 4. Calculate initial/final balances
     * 5. Generate summary (total credits, debits, net change)
     * 6. Return complete report
     *
     * @param customerId Customer ID
     * @param startDate Start date of report period
     * @param endDate End date of report period
     * @return Mono<AccountStatementReport> Complete statement report
     * <p>
     */
    Mono<AccountStatementReport> generateAccountStatement(UUID customerId, LocalDate startDate, LocalDate endDate);
}
