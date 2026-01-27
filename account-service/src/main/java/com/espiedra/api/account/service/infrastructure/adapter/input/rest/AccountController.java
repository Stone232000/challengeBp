package com.espiedra.api.account.service.infrastructure.adapter.input.rest;

import com.espiedra.api.account.service.application.port.input.*;
import com.espiedra.api.account.service.infrastructure.adapter.input.rest.mapper.AccountApiMapper;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.api.AccountsApi;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

/**
 * REST CONTROLLER: AccountController
 * <p>
 * Primary/Driving Adapter in Hexagonal Architecture.
 * Handles HTTP requests for account management operations.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts HTTP/REST to domain use cases
 * - Anti-Corruption Layer: Maps between OpenAPI models and Domain models
 * - Facade Pattern: Provides unified interface to multiple use cases
 * - Controller Pattern: Handles HTTP request/response
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles HTTP/REST concerns
 * - Dependency Inversion: Depends on use case abstractions (ports)
 * - Interface Segregation: Implements OpenAPI-generated interface
 * <p>
 * FEATURES:
 * - Reactive endpoints (returns Mono/Flux)
 * - OpenAPI Contract-First approach
 * - Proper HTTP status codes
 * - Location header for created resources
 * - Correlation ID and Request ID propagation
 * - MapStruct for DTO mapping
 * <p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController implements AccountsApi {

    // ========================================================================
    // DEPENDENCY INJECTION (Use Cases / Input Ports)
    // ========================================================================

    private final CreateAccountUseCase createAccountUseCase;
    private final QueryAccountUseCase queryAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;

    // Mapper for OpenAPI <-> Domain conversion
    private final AccountApiMapper accountApiMapper;

    // ========================================================================
    // OPENAPI INTERFACE IMPLEMENTATION
    // ========================================================================

    /**
     * POST /accounts
     * Create a new bank account
     * <p>
     * Implementation of OpenAPI-generated interface method.
     * <p>
     * BUSINESS FLOW:
     * 1. Map AccountCreateRequest (OpenAPI) to Account (Domain)
     * 2. Call CreateAccountUseCase
     * 3. Map Account (Domain) to AccountResponse (OpenAPI)
     * 4. Return 201 CREATED with Location header
     *
     * @param xRequestId ID único de la petición para trazabilidad
     * @param accountCreateRequest Datos de la cuenta a crear
     * @param xCorrelationId ID de correlación para trazar operaciones entre servicios
     * @param idempotencyKey Clave de idempotencia para operaciones críticas
     * @param exchange ServerWebExchange for reactive context
     * @return Mono<ResponseEntity<AccountResponse>> 201 with created account
     */
    @Override
    public Mono<ResponseEntity<AccountResponse>> createAccount(
            UUID xRequestId,
            AccountCreateRequest accountCreateRequest,
            UUID xCorrelationId,
            UUID idempotencyKey,
            ServerWebExchange exchange
    ) {
        log.info("Creating account for customer: {} [requestId={}, correlationId={}, idempotencyKey={}]",
                accountCreateRequest.getCustomerId(), xRequestId, xCorrelationId, idempotencyKey);

        // Map OpenAPI request to Domain model
        var accountDomain = accountApiMapper.toDomain(accountCreateRequest);

        return createAccountUseCase.createAccount(accountDomain)
                .map(created -> {
                    // Map Domain model to OpenAPI response
                    var response = accountApiMapper.toResponse(created);

                    return ResponseEntity
                            .created(URI.create("/accounts/" + created.getAccountNumber()))
                            .body(response);
                })
                .doOnSuccess(response -> log.info("Account created successfully: {} [requestId={}]",
                        response.getBody().getAccountNumber(), xRequestId))
                .doOnError(error -> log.error("Error creating account [requestId={}]: {}",
                        xRequestId, error.getMessage()));
    }

    /**
     * DELETE /accounts/{accountNumber}
     * Delete an account (soft delete)
     *
     * @param accountNumber Número de cuenta bancaria
     * @param xRequestId ID único de la petición
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<Void>> 204 NO CONTENT
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(
            Long accountNumber,
            UUID xRequestId,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.info("Deleting account: {} [requestId={}, correlationId={}]",
                accountNumber, xRequestId, xCorrelationId);

        return deleteAccountUseCase.deleteAccount(accountNumber)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnSuccess(v -> log.info("Account deleted successfully: {} [requestId={}]",
                        accountNumber, xRequestId))
                .doOnError(error -> log.error("Error deleting account {} [requestId={}]: {}",
                        accountNumber, xRequestId, error.getMessage()));
    }

    /**
     * GET /accounts/{accountNumber}/balance
     * Get current balance for an account
     *
     * @param accountNumber Número de cuenta bancaria
     * @param xRequestId ID único de la petición
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<BalanceResponse>> 200 with balance
     */
    @Override
    public Mono<ResponseEntity<BalanceResponse>> getAccountBalance(
            Long accountNumber,
            UUID xRequestId,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.debug("Getting balance for account: {} [requestId={}]", accountNumber, xRequestId);

        return queryAccountUseCase.getAccount(accountNumber)
                .map(account -> {
                    BalanceResponse response = new BalanceResponse()
                            .accountNumber(account.getAccountNumber())
                            .balance(account.getBalance().doubleValue())
                            .availableBalance(account.getAvailableBalance().doubleValue());

                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * GET /accounts/{accountNumber}
     * Get account by account number
     *
     * @param accountNumber Número de cuenta bancaria
     * @param xRequestId ID único de la petición
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<AccountResponse>> 200 with account or 404
     */
    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccountByNumber(
            Long accountNumber,
            UUID xRequestId,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.debug("Getting account: {} [requestId={}]", accountNumber, xRequestId);

        return queryAccountUseCase.getAccount(accountNumber)
                .map(account -> {
                    var response = accountApiMapper.toResponse(account);
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * GET /accounts
     * Get all accounts with optional filtering
     *
     * @param xRequestId ID único de la petición
     * @param page Número de página
     * @param size Tamaño de página
     * @param sort Ordenamiento
     * @param customerId Filtrar por ID de cliente
     * @param accountType Filtrar por tipo de cuenta
     * @param state Filtrar por estado
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<AccountPageResponse>> Paginated account list
     */
    @Override
    public Mono<ResponseEntity<AccountPageResponse>> getAccounts(
            UUID xRequestId,
            Integer page,
            Integer size,
            String sort,
            UUID customerId,
            AccountType accountType,
            Boolean state,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.debug("Getting accounts [customerId={}, accountType={}, state={}, page={}, size={}, requestId={}]",
                customerId, accountType, state, page, size, xRequestId);

        // Determine which use case to call based on customerId
        Flux<com.espiedra.api.account.service.domain.model.Account> accountsFlux = customerId != null
                ? queryAccountUseCase.getAccountsByCustomer(customerId)
                : queryAccountUseCase.getAllAccounts();

        // Apply filters
        return accountsFlux
                .filter(account -> {
                    // Apply accountType filter if provided
                    if (accountType != null) {
                        String domainAccountType = account.getAccountType().name();
                        String filterAccountType = accountType.name();
                        if (!domainAccountType.equals(filterAccountType)) {
                            return false;
                        }
                    }

                    // Apply state filter if provided
                    if (state != null && !state.equals(account.getState())) {
                        return false;
                    }

                    return true;
                })
                .map(accountApiMapper::toResponse)
                .collectList()
                .map(accounts -> {
                    PageMetadata pageMetadata = new PageMetadata()
                            .size(accounts.size())
                            .number(page != null ? page : 0)
                            .totalElements(accounts.size())
                            .totalPages(accounts.isEmpty() ? 0 : 1);

                    AccountPageResponse pageResponse = new AccountPageResponse()
                            .content(accounts)
                            .page(pageMetadata);

                    return ResponseEntity.ok(pageResponse);
                })
                .doOnSuccess(response -> log.debug("Retrieved {} accounts [requestId={}]",
                        response.getBody().getContent().size(), xRequestId));
    }

    /**
     * PATCH /accounts/{accountNumber}/state
     * Partially update account state
     *
     * @param accountNumber Número de cuenta bancaria
     * @param xRequestId ID único de la petición
     * @param accountStatePatchRequest Estado a actualizar
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<AccountResponse>> 200 with updated account
     */
    @Override
    public Mono<ResponseEntity<AccountResponse>> patchAccountState(
            Long accountNumber,
            UUID xRequestId,
            AccountStatePatchRequest accountStatePatchRequest,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.info("Patching account state: {} to {} [requestId={}]",
                accountNumber, accountStatePatchRequest.getState(), xRequestId);

        return queryAccountUseCase.getAccount(accountNumber)
                .flatMap(account -> {
                    // Update only the state
                    account.setState(accountStatePatchRequest.getState());
                    return updateAccountUseCase.updateAccount(accountNumber, account);
                })
                .map(updated -> {
                    var response = accountApiMapper.toResponse(updated);
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
