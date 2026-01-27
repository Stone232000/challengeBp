package com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC Entity - Customer
 * <p>
 * Maps to the "customer" table in PostgresSQL.
 * This is an infrastructure concern, separate from domain models.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This entity belongs to the INFRASTRUCTURE layer (Secondary Adapter).
 * It will be mapped to/from domain models using MapStruct.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Entity Pattern: Represents a database table row
 * - Builder Pattern: Lombok @Builder for object construction
 * - Optimistic Locking Pattern: @Version annotation for concurrency control
 * - Persistable Pattern: Implements Persistable to control INSERT vs UPDATE behavior
 * <p>
 * SOLID PRINCIPLES:
 * - SRP: Single responsibility - represents a customer entity
 * - OCP: Open for extension through Persistable interface
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("customer")
public class CustomerEntity implements Persistable<UUID> {

    @Id
    @Column("customer_id")
    private UUID customerId;

    @Column("person_id")
    private UUID personId;

    @Column("password")
    private String password;

    @Column("state")
    private Boolean state;

    /**
     * Optimistic locking version field.
     * Spring Data R2DBC automatically increments this on updates.
     */
    @Version
    @Column("version")
    private Long version;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Transient field to track if this entity is new.
     * Used by Spring Data R2DBC to determine INSERT vs UPDATE.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return customerId;
    }

    @Override
    @Transient
    public boolean isNew() {
        return isNew || createdAt == null;
    }

    /**
     * Mark this entity as persisted (not new).
     * Call this after successful save to prevent duplicate inserts.
     */
    public void markAsNotNew() {
        this.isNew = false;
    }
}
