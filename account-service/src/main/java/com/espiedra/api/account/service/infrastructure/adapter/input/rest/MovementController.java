package com.espiedra.api.account.service.infrastructure.adapter.input.rest;

import com.espiedra.api.account.service.application.port.input.CreateMovementUseCase;
import com.espiedra.api.account.service.application.port.output.MovementRepositoryPort;
import com.espiedra.api.account.service.infrastructure.adapter.input.rest.mapper.MovementApiMapper;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.api.MovementsApi;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST CONTROLLER: MovementController
 * <p>
 * Primary/Driving Adapter in Hexagonal Architecture.
 * Handles HTTP requests for movement/transaction operations.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Adapter Pattern: Adapts HTTP/REST to domain use cases
 * - Anti-Corruption Layer: Maps between OpenAPI models and Domain models
 * - Controller Pattern: Handles HTTP request/response
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles HTTP/REST concerns for movements
 * - Dependency Inversion: Depends on use case abstractions
 * - Interface Segregation: Implements OpenAPI-generated interface
 * <p>
 * FEATURES:
 * - Reactive endpoints (returns Mono)
 * - OpenAPI Contract-First approach
 * - Idempotency via transaction ID
 * - Proper HTTP status codes (201 for creation)
 * - Location header for created resources
 * - MapStruct for DTO mapping
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MovementController implements MovementsApi {

    private final CreateMovementUseCase createMovementUseCase;
    private final MovementRepositoryPort movementRepositoryPort;

    // Mapper for OpenAPI <-> Domain conversion
    private final MovementApiMapper movementApiMapper;

    /**
     * POST /movements
     * Create a new movement (deposit or withdrawal)
     *
     * Implementation of OpenAPI-generated interface method.
     *
     * BUSINESS FLOW:
     * 1. Map MovementCreateRequest (OpenAPI) to Movement (Domain)
     * 2. Validate transaction ID uniqueness (idempotency)
     * 3. Validate account exists and is active
     * 4. Validate sufficient balance (for withdrawals)
     * 5. Update account balance
     * 6. Create movement record
     * 7. Map Movement (Domain) to MovementResponse (OpenAPI)
     * 8. Return 201 CREATED
     *
     * @param xRequestId ID único de la petición
     * @param movementCreateRequest Datos del movimiento a crear
     * @param xCorrelationId ID de correlación
     * @param idempotencyKey Clave de idempotencia
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<MovementResponse>> 201 with created movement
     */
    @Override
    public Mono<ResponseEntity<MovementResponse>> createMovement(
            UUID xRequestId,
            MovementCreateRequest movementCreateRequest,
            UUID xCorrelationId,
            UUID idempotencyKey,
            ServerWebExchange exchange
    ) {
        log.info("Creating movement: type={}, account={}, amount={} [requestId={}, correlationId={}, idempotencyKey={}]",
                movementCreateRequest.getMovementType(),
                movementCreateRequest.getAccountNumber(),
                movementCreateRequest.getAmount(),
                xRequestId,
                xCorrelationId,
                idempotencyKey);

        // Map OpenAPI request to Domain model
        var movementDomain = movementApiMapper.toDomain(movementCreateRequest);

        // Generate transaction ID if not provided (auto-generate using timestamp + random)
        if (movementDomain.getTransactionId() == null) {
            String transactionId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            movementDomain.setTransactionId(transactionId);
            log.debug("Generated transaction ID: {}", transactionId);
        }

        // Set idempotency key and correlation/request IDs
        if (idempotencyKey != null) {
            movementDomain.setIdempotencyKey(idempotencyKey);
        }
        if (xCorrelationId != null) {
            movementDomain.setCorrelationId(xCorrelationId);
        }
        if (xRequestId != null) {
            movementDomain.setRequestId(xRequestId);
        }

        return createMovementUseCase.createMovement(movementDomain)
                .map(created -> {
                    // Map Domain model to OpenAPI response
                    var response = movementApiMapper.toResponse(created);

                    return ResponseEntity
                            .created(URI.create("/movements/" + created.getMovementId()))
                            .body(response);
                })
                .doOnSuccess(response -> log.info("Movement created successfully: movementId={}, balanceAfter={} [requestId={}]",
                        response.getBody().getMovementId(),
                        response.getBody().getBalanceAfter(),
                        xRequestId))
                .doOnError(error -> log.error("Error creating movement [requestId={}]: {}",
                        xRequestId, error.getMessage()));
    }

    /**
     * GET /movements/{movementId}
     * Get movement by ID
     *
     * @param movementId ID del movimiento
     * @param xRequestId ID único de la petición
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<MovementResponse>> 200 with movement or 404
     */
    @Override
    public Mono<ResponseEntity<MovementResponse>> getMovementById(
            UUID movementId,
            UUID xRequestId,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.info("Getting movement by ID: {} [requestId={}]", movementId, xRequestId);

        return movementRepositoryPort.findByMovementId(movementId)
                .map(movement -> {
                    var response = movementApiMapper.toResponse(movement);
                    log.debug("Movement found: movementId={}, accountNumber={} [requestId={}]",
                            movementId, movement.getAccountNumber(), xRequestId);
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.warn("Movement not found: movementId={} [requestId={}]", movementId, xRequestId);
                    }
                });
    }

    /**
     * GET /movements
     * Get all movements with optional filtering
     *
     * @param xRequestId ID único de la petición
     * @param page Número de página
     * @param size Tamaño de página
     * @param sort Ordenamiento
     * @param accountNumber Filtrar por número de cuenta
     * @param movementType Filtrar por tipo de movimiento
     * @param startDate Fecha inicio para filtrado
     * @param endDate Fecha fin para filtrado
     * @param xCorrelationId ID de correlación
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<MovementPageResponse>> Paginated movement list
     */
    @Override
    public Mono<ResponseEntity<MovementPageResponse>> getMovements(
            UUID xRequestId,
            Integer page,
            Integer size,
            String sort,
            Long accountNumber,
            MovementType movementType,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            UUID xCorrelationId,
            ServerWebExchange exchange
    ) {
        log.debug("Getting movements [accountNumber={}, movementType={}, startDate={}, endDate={}, page={}, size={}, requestId={}]",
                accountNumber, movementType, startDate, endDate, page, size, xRequestId);

        // Validate accountNumber is required
        if (accountNumber == null) {
            log.warn("AccountNumber is required for getting movements [requestId={}]", xRequestId);
            PageMetadata pageMetadata = new PageMetadata()
                    .size(size != null ? size : 20)
                    .number(page != null ? page : 0)
                    .totalElements(0)
                    .totalPages(0);

            MovementPageResponse emptyResponse = new MovementPageResponse()
                    .content(java.util.List.of())
                    .page(pageMetadata);

            return Mono.just(ResponseEntity.ok(emptyResponse));
        }

        // Determine which repository method to call
        reactor.core.publisher.Flux<com.espiedra.api.account.service.domain.model.Movement> movementsFlux;

        if (startDate != null && endDate != null) {
            // Use date range filtering
            java.time.LocalDateTime start = startDate.toLocalDateTime();
            java.time.LocalDateTime end = endDate.toLocalDateTime();
            movementsFlux = movementRepositoryPort.findByAccountNumberAndDateRange(accountNumber, start, end);
        } else {
            // Get all movements for account
            movementsFlux = movementRepositoryPort.findByAccountNumber(accountNumber);
        }

        // Apply movement type filter if provided
        if (movementType != null) {
            movementsFlux = movementsFlux.filter(movement ->
                movement.getMovementType().name().equals(movementType.name())
            );
        }

        // Apply pagination (skip + limit) - use final variables for lambda
        final int finalPageNumber = (page != null && page >= 0) ? page : 0;
        final int finalPageSize = (size != null && size > 0 && size <= 100) ? size : 20;
        final int skip = finalPageNumber * finalPageSize;
        final int limit = finalPageSize;

        // Count total elements for pagination metadata
        return movementsFlux
                .map(movementApiMapper::toResponse)
                .collectList()
                .map(allMovements -> {
                    int totalElements = allMovements.size();
                    int totalPages = (int) Math.ceil((double) totalElements / finalPageSize);

                    // Apply pagination: skip first N elements and take limit
                    java.util.List<MovementResponse> paginatedMovements = allMovements.stream()
                            .skip(skip)
                            .limit(limit)
                            .collect(java.util.stream.Collectors.toList());

                    PageMetadata pageMetadata = new PageMetadata()
                            .size(finalPageSize)
                            .number(finalPageNumber)
                            .totalElements(totalElements)
                            .totalPages(totalPages);

                    MovementPageResponse pageResponse = new MovementPageResponse()
                            .content(paginatedMovements)
                            .page(pageMetadata);

                    log.debug("Retrieved {} movements (page {}/{}, total: {}) [requestId={}]",
                            paginatedMovements.size(), finalPageNumber, totalPages, totalElements, xRequestId);

                    return ResponseEntity.ok(pageResponse);
                })
                .doOnSuccess(response -> log.debug("Pagination applied: page={}, size={}, returned {} items [requestId={}]",
                        finalPageNumber, finalPageSize, response.getBody().getContent().size(), xRequestId));
    }

    /**
     * POST /movements/{movementId}/reverse
     * Reverse a movement (undo transaction)
     *
     * @param movementId ID del movimiento a reversar
     * @param xRequestId ID único de la petición
     * @param movementReverseRequest Datos de la reversión
     * @param xCorrelationId ID de correlación
     * @param idempotencyKey Clave de idempotencia
     * @param exchange ServerWebExchange
     * @return Mono<ResponseEntity<MovementResponse>> 201 with reversal movement
     */
    @Override
    public Mono<ResponseEntity<MovementResponse>> reverseMovement(
            UUID movementId,
            UUID xRequestId,
            MovementReverseRequest movementReverseRequest,
            UUID xCorrelationId,
            UUID idempotencyKey,
            ServerWebExchange exchange
    ) {
        log.info("Reversing movement: {} with reason: {} [requestId={}, idempotencyKey={}]",
                movementId, movementReverseRequest.getReason(), xRequestId, idempotencyKey);

        // Step 1: Get the original movement to reverse
        return movementRepositoryPort.findByMovementId(movementId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Movement not found: " + movementId)))
                .flatMap(originalMovement -> {
                    // Validate that movement has not been reversed already
                    if (Boolean.TRUE.equals(originalMovement.getReversed())) {
                        return Mono.error(new IllegalStateException(
                                "Movement already reversed: " + movementId));
                    }

                    // Create reversal movement using domain model
                    var reversalMovement = com.espiedra.api.account.service.domain.model.Movement.builder()
                            .accountNumber(originalMovement.getAccountNumber())
                            .movementType(com.espiedra.api.account.service.domain.model.MovementType.REVERSA)
                            .amount(originalMovement.getAmount())
                            .description("Reversa: " + (movementReverseRequest.getReason() != null
                                    ? movementReverseRequest.getReason()
                                    : "Reversión de movimiento"))
                            .reference(originalMovement.getTransactionId())
                            .reversedMovementId(movementId)
                            .transactionId(generateTransactionId())
                            .idempotencyKey(idempotencyKey)
                            .correlationId(xCorrelationId)
                            .requestId(xRequestId)
                            .build();

                    log.debug("Creating reversal movement for original movement: {} [requestId={}]",
                            movementId, xRequestId);

                    // Use CreateMovementUseCase to create the reversal
                    return createMovementUseCase.createMovement(reversalMovement);
                })
                .flatMap(createdReversal -> {
                    // Re-fetch the movement to get the updated balance_after from the database trigger
                    return movementRepositoryPort.findByMovementId(createdReversal.getMovementId())
                            .map(refreshedMovement -> {
                                var response = movementApiMapper.toResponse(refreshedMovement);

                                // Build Location header
                                URI location = URI.create("/api/v1/movements/" + refreshedMovement.getMovementId());

                                log.info("Movement reversed successfully: originalMovement={}, reversalMovement={}, finalBalance={} [requestId={}]",
                                        movementId, refreshedMovement.getMovementId(), refreshedMovement.getBalanceAfter(), xRequestId);

                                return ResponseEntity.created(location).body(response);
                            });
                })
                .doOnError(error -> log.error("Error reversing movement {} [requestId={}]: {}",
                        movementId, xRequestId, error.getMessage()));
    }

    /**
     * Generate a unique transaction ID
     *
     * @return Transaction ID
     */
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
