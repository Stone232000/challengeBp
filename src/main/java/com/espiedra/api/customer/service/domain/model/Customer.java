package com.espiedra.api.customer.service.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Model - Customer (Aggregate Root)
 * <p>
 * Represents a customer which EXTENDS Person (IS-A relationship).
 * A Customer IS-A Person with additional authentication information.
 * This follows the requirement: "Client inheritance person"
 * <p>
 * SOLID PRINCIPLES APPLIED:
 * - Single Responsibility Principle (SRP): Manages customer-specific data and business logic
 * - Liskov Substitution Principle (LSP): Customer IS-A Person and can be substituted
 * - Open/Closed Principle (OCP): Extends Person without modifying it
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Aggregate Root Pattern: Customer is the entry point for all operations
 * - Builder Pattern: SuperBuilder for inheritance-friendly fluent construction
 * - Inheritance Pattern: Customer extends Person (as per requirements)
 * <p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends Person {

    /**
     * Unique identifier for the customer (same as Person ID).
     * This creates a 1-1 relationship between Customer and Person.
     */
    private UUID customerId;

    /**
     * Encrypted password for authentication.
     * Should be hashed using BCrypt.
     * Not exposed in DTOs (security concern).
     */
    private String password;

    /**
     * Customer account state (active/inactive).
     * true = active, false = inactive
     */
    private Boolean state;

    /**
     * Version field for Optimistic Locking.
     * Prevents lost updates in concurrent scenarios.
     * <p>
     * PATTERN: Optimistic Locking Pattern for concurrency control
     */
    private Long version;

    /**
     * Factory Method Pattern: Create a new Customer with Person information
     *
     * @param name Person name
     * @param gender Person gender
     * @param age Person age
     * @param identification Person identification
     * @param address Person address
     * @param phone Person phone
     * @param encodedPassword BCrypt encoded password
     * @return Customer instance with generated UUID, active state, and timestamps
     */
    public static Customer create(String name, GenderType gender, Integer age, String identification,
                                   String address, String phone, String encodedPassword) {
        UUID customerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        return Customer.builder()
                .customerId(customerId)
                .personId(customerId) // Same ID for Person and Customer
                .name(name)
                .gender(gender)
                .age(age)
                .identification(identification)
                .address(address)
                .phone(phone)
                .password(encodedPassword)
                .state(true) // Active by default
                .version(0L) // Initial version for optimistic locking
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Domain Business Logic: Activate customer account
     * <p>
     * SOLID: Single Responsibility - Encapsulates state change logic
     *
     * @return Activated customer instance
     */
    public Customer activate() {
        return this.toBuilder()
                .state(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Domain Business Logic: Deactivate customer account
     *
     * @return Deactivated customer instance
     */
    public Customer deactivate() {
        return this.toBuilder()
                .state(false)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Domain Business Logic: Update customer password
     *
     * @param newEncodedPassword New BCrypt encoded password
     * @return Customer with updated password
     */
    public Customer updatePassword(String newEncodedPassword) {
        return this.toBuilder()
                .password(newEncodedPassword)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Domain Business Logic: Check if customer is active
     *
     * @return true if state is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.state);
    }

    /**
     * Override: Validate if customer (as a person) is an adult
     * Inherited from Person, can be used polymorphically
     *
     * @return true if age >= 18
     */
    @Override
    public boolean isAdult() {
        return super.isAdult();
    }

    /**
     * Override: Check if identification is valid
     * Inherited from Person, can be used polymorphically
     *
     * @return true if identification matches pattern
     */
    @Override
    public boolean hasValidIdentification() {
        return super.hasValidIdentification();
    }
}
