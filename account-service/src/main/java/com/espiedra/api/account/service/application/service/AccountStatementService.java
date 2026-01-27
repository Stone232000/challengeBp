package com.espiedra.api.account.service.application.service;

import com.espiedra.api.account.service.application.dto.*;
import com.espiedra.api.account.service.application.port.input.GenerateAccountStatementUseCase;
import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.application.port.output.CustomerServiceClientPort;
import com.espiedra.api.account.service.application.port.output.MovementRepositoryPort;
import com.espiedra.api.account.service.domain.exception.CustomerNotActiveException;
import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.domain.model.Customer;
import com.espiedra.api.account.service.domain.model.Movement;
import com.espiedra.api.account.service.domain.model.MovementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * APPLICATION SERVICE: AccountStatementService
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Service Pattern: Encapsulates statement generation operations
 * - Query Pattern: Read-only reporting operation
 * - DTO Pattern: Returns structured report DTOs
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles account statement generation
 * - Dependency Inversion: Depends on abstractions (ports)
 * <p>
 * BUSINESS CAPABILITIES:
 * - Generate Account Statement Report
 * <p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountStatementService implements GenerateAccountStatementUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final MovementRepositoryPort movementRepositoryPort;
    private final CustomerServiceClientPort customerServiceClientPort;

    /**
     * Generate account statement report for a customer
     * <p>
     * BUSINESS FLOW:
     * 1. Validate customer exists (via cache or WebClient)
     * 2. Retrieve all customer accounts
     * 3. For each account, get movements in date range
     * 4. Calculate initial/final balances
     * 5. Generate summary (total credits, debits, net change)
     * 6. Return complete report
     * <p>
     * PATTERN: Query Pattern - read-only reporting operation
     * PATTERN: DTO Pattern - returns structured report DTO
     * SOLID: Single Responsibility - only generates statements
     *
     * @param customerId Customer ID
     * @param startDate  Start date of report period
     * @param endDate    End date of report period
     * @return Mono<AccountStatementReport> Complete statement report
     */
    @Override
    public Mono<AccountStatementReport> generateAccountStatement(UUID customerId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating account statement: customerId={}, period={} to {}", customerId, startDate, endDate);

        // BUSINESS RULE: Validate date range
        if (startDate.isAfter(endDate)) {
            log.warn("Invalid date range: startDate={} is after endDate={}", startDate, endDate);
            return Mono.error(new IllegalArgumentException("Start date must be before or equal to end date"));
        }

        return validateCustomerResilient(customerId)
                .flatMap(customer -> generateStatement(customer, startDate, endDate))
                .doOnSuccess(report -> log.info("Account statement generated: reportId={}, customerId={}, totalAccounts={}",
                        report.reportId(), customerId, report.summary().totalAccounts()))
                .doOnError(error -> log.error("Error generating statement for customer {}: {}", customerId, error.getMessage()));
    }

    /**
     * Generate the complete statement report
     *
     * @param customer  Customer information
     * @param startDate Start date
     * @param endDate   End date
     * @return Mono<AccountStatementReport> Complete report
     */
    private Mono<AccountStatementReport> generateStatement(Customer customer, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        return accountRepositoryPort.findByCustomerId(customer.getCustomerId())
                .collectList()
                .flatMap(accounts -> {
                    if (accounts.isEmpty()) {
                        log.debug("No accounts found for customer: {}", customer.getCustomerId());
                        return Mono.just(createEmptyStatement(customer, startDate, endDate));
                    }

                    // For each account, get movements and build statement
                    return Flux.fromIterable(accounts)
                            .flatMap(account -> buildAccountStatement(account, startDateTime, endDateTime))
                            .collectList()
                            .map(accountStatements -> buildCompleteReport(customer, startDate, endDate, accountStatements));
                });
    }

    /**
     * Build statement for a single account
     *
     * @param account       Account
     * @param startDateTime Start date/time
     * @param endDateTime   End date/time
     * @return Mono<AccountStatement> Account statement
     */
    private Mono<AccountStatement> buildAccountStatement(Account account, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return movementRepositoryPort.findByAccountNumberAndDateRange(
                        account.getAccountNumber(), startDateTime, endDateTime)
                .collectList()
                .map(movements -> {
                    List<MovementDetail> movementDetails = movements.stream()
                            .map(this::buildMovementDetail)
                            .toList();

                    // Calculate initial and final balances for the period
                    BigDecimal initialBalance = calculateInitialBalance(account, movements);
                    BigDecimal finalBalance = account.getBalance();

                    return new AccountStatement(
                            account.getAccountNumber(),
                            account.getAccountType().name(),
                            account.getState(),
                            initialBalance,
                            finalBalance,
                            movementDetails
                    );
                });
    }

    /**
     * Build movement detail DTO
     *
     * @param movement Movement
     * @return MovementDetail DTO
     */
    private MovementDetail buildMovementDetail(Movement movement) {
        // Use ISO_LOCAL_DATE_TIME format for consistency with DateTimeMapper
        // Format: "2025-11-10T22:21:42" (with 'T' separator)
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String description = movement.getMovementType() == MovementType.CREDITO
                ? String.format("Crédito de %s", movement.getAmount())
                : String.format("Débito de %s", movement.getAmount());

        return new MovementDetail(
                movement.getCreatedAt().format(formatter),
                movement.getMovementType().name(),
                description,
                movement.getAmount(),
                movement.getBalanceAfter(),
                movement.getTransactionId()
        );
    }

    /**
     * Calculate initial balance for the period
     * Initial balance = current balance - net change during period
     *
     * @param account   Account
     * @param movements Movements in period
     * @return BigDecimal Initial balance
     */
    private BigDecimal calculateInitialBalance(Account account, List<Movement> movements) {
        BigDecimal netChange = movements.stream()
                .map(Movement::getNetEffect)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return account.getBalance().subtract(netChange);
    }

    /**
     * Build complete statement report
     *
     * @param customer          Customer
     * @param startDate         Start date
     * @param endDate           End date
     * @param accountStatements Account statements
     * @return AccountStatementReport Complete report
     */
    private AccountStatementReport buildCompleteReport(
            Customer customer, LocalDate startDate, LocalDate endDate,
            List<AccountStatement> accountStatements) {

        // Calculate summary
        BigDecimal totalCredits = accountStatements.stream()
                .flatMap(as -> as.movements().stream())
                .filter(m -> m.movementType().equals(MovementType.CREDITO.name()))
                .map(MovementDetail::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = accountStatements.stream()
                .flatMap(as -> as.movements().stream())
                .filter(m -> m.movementType().equals(MovementType.DEBITO.name()))
                .map(MovementDetail::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalMovements = accountStatements.stream()
                .mapToInt(as -> as.movements().size())
                .sum();

        BigDecimal netChange = totalCredits.subtract(totalDebits);

        StatementSummary summary = new StatementSummary(
                accountStatements.size(),
                totalCredits,
                totalDebits,
                totalMovements,
                netChange
        );

        return new AccountStatementReport(
                UUID.randomUUID(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                new CustomerInfo(customer.getCustomerId(), customer.getName(), customer.getIdentification()),
                new ReportPeriod(startDate.toString(), endDate.toString()),
                accountStatements,
                summary
        );
    }

    /**
     * Create empty statement when customer has no accounts
     *
     * @param customer  Customer
     * @param startDate Start date
     * @param endDate   End date
     * @return AccountStatementReport Empty report
     */
    private AccountStatementReport createEmptyStatement(Customer customer, LocalDate startDate, LocalDate endDate) {

        StatementSummary summary = new StatementSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);

        return new AccountStatementReport(
                UUID.randomUUID(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                new CustomerInfo(customer.getCustomerId(), customer.getName(), customer.getIdentification()),
                new ReportPeriod(startDate.toString(), endDate.toString()),
                List.of(),
                summary
        );
    }

    /**
     * Validate customer exists and is active using WebClient
     * <p>
     * VALIDATION STRATEGY: Direct HTTP call to Customer Service
     * - Calls customer-service via WebClient
     * - Validates customer exists and is active
     * - Timeout of 3 seconds for resilience
     * <p>
     * NOTE: Database cache removed - validation done via HTTP only
     * Optional: Add Caffeine/Redis cache for better performance
     *
     * @param customerId Customer ID to validate
     * @return Mono<Customer> Validated customer
     */
    private Mono<Customer> validateCustomerResilient(UUID customerId) {
        log.debug("Validating customer via WebClient: {}", customerId);

        return customerServiceClientPort.validateCustomer(customerId)
                .flatMap(this::validateCustomerState)
                .doOnSuccess(customer -> log.debug("Customer {} validated successfully", customerId))
                .doOnError(error -> log.error("Error validating customer {}: {}", customerId, error.getMessage()));
    }

    /**
     * Validate customer state (must be active)
     * <p>
     * BUSINESS RULE: Customer must be active (state = TRUE)
     *
     * @param customer Customer to validate
     * @return Mono<Customer> Customer if active
     */
    private Mono<Customer> validateCustomerState(Customer customer) {
        if (!customer.isActive()) {
            log.warn("Customer {} is not active, validation failed", customer.getCustomerId());
            return Mono.error(new CustomerNotActiveException(customer.getCustomerId()));
        }
        log.debug("Customer {} is active, validation passed", customer.getCustomerId());
        return Mono.just(customer);
    }
}
