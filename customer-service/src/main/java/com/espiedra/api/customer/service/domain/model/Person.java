package com.espiedra.api.customer.service.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Model - Person (Base Class)
 * <p>
 * Represents a physical person in the system.
 * This is a DDD Entity with identity (UUID) and lifecycle.
 * <p>
 * This class serves as the base class for Customer (inheritance).
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): This class only handles person data and business logic
 * - Open/Closed Principle (OCP): Can be extended without modification (Customer extends this)
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Builder Pattern: SuperBuilder for inheritance-friendly fluent object construction
 * - Template Method Pattern: Provides base behavior that subclasses can extend
 * <p>
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    /**
     * Unique identifier for the person (UUID).
     * This is the identity of the entity in DDD terms.
     */
    private UUID personId;

    /**
     * Full name of the person.
     */
    private String name;

    /**
     * Gender of the person.
     * Expected values: MASCULINO, FEMENINO, OTRO
     */
    private GenderType gender;

    /**
     * Age of the person.
     */
    private Integer age;

    /**
     * National identification number (cedula/passport).
     * Must be unique in the system.
     */
    private String identification;

    /**
     * Physical address of the person.
     */
    private String address;

    /**
     * Contact phone number.
     */
    private String phone;

    /**
     * Timestamp when the person was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the person was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Factory Method Pattern: Create a new Person with default values
     *
     * @return Person instance with generated UUID and timestamps
     */
    public static Person create() {
        return Person.builder()
                .personId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Domain Business Logic: Validate if person is an adult
     *
     * @return true if age >= 18
     */
    public boolean isAdult() {
        return this.age != null && this.age >= 18;
    }

    /**
     * Domain Business Logic: Check if identification is valid format
     *
     * @return true if identification matches pattern
     */
    public boolean hasValidIdentification() {
        return this.identification != null &&
               this.identification.length() >= 10 &&
               this.identification.length() <= 20;
    }
}
