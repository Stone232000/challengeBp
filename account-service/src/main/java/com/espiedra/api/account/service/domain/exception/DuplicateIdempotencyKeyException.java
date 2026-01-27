package com.espiedra.api.account.service.domain.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * DOMAIN EXCEPTION: DuplicateIdempotencyKeyException
 * <p>
 * Thrown when attempting to create a movement with an idempotency key that already exists.
 * This exception ensures idempotency in movement creation operations.
 * <p>
 * PATTERN: Domain Exception
 * HTTP STATUS: 409 CONFLICT
 * <p>
 * USE CASE:
 * - Prevent duplicate transaction processing
 * - Ensure exactly-once semantics in distributed systems
 * - Handle retry scenarios gracefully
 * <p>
 */
@Getter
public class DuplicateIdempotencyKeyException extends RuntimeException {

    private final UUID idempotencyKey;
    private final UUID existingMovementId;

    public DuplicateIdempotencyKeyException(UUID idempotencyKey, UUID existingMovementId) {
        super(String.format("Movement with idempotency key '%s' already exists (movementId: %s). " +
                        "This request has already been processed.",
                idempotencyKey, existingMovementId));
        this.idempotencyKey = idempotencyKey;
        this.existingMovementId = existingMovementId;
    }

}
