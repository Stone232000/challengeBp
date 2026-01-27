package com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC Entity - Person
 * <p>
 * Maps to the "person" table in PostgresSQL.
 * This is an infrastructure concern, separate from domain models.
 * <p>
 * HEXAGONAL ARCHITECTURE:
 * This entity belongs to the INFRASTRUCTURE layer (Secondary Adapter).
 * It will be mapped to/from domain models using MapStruct.
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Entity Pattern: Represents a database table row
 * - Builder Pattern: Lombok @Builder for object construction
 * - Persistable Pattern: Implements Persistable to control INSERT vs UPDATE behavior
 * <p>
 * SOLID PRINCIPLES:
 * - SRP: Single responsibility - represents a person entity
 * - OCP: Open for extension through Persistable interface
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("person")
public class PersonEntity implements Persistable<UUID> {

    @Id
    @Column("person_id")
    private UUID personId;

    @Column("name")
    private String name;

    @Column("gender")
    private String gender;

    @Column("age")
    private Integer age;

    @Column("identification")
    private String identification;

    @Column("address")
    private String address;

    @Column("phone")
    private String phone;

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
        return personId;
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
