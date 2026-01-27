package com.espiedra.api.account.service.infrastructure.adapter.output.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ENTITY: AccountEntity (R2DBC)
 * <p>
 * PURPOSE:
 * - Represents the database table structure
 * - Handles R2DBC persistence operations
 * - Separated from domain logic
 * <p>
 * DESIGN PATTERNS:
 * - Entity Pattern: Represents database table structure
 * - Adapter Pattern: Adapts domain to infrastructure
 * <p>
 * MAPPING:
 * - Table: account
 * - Primary Key: account_number (Long, auto-generated via sequence)
 * <p>
 * NOTE: Spring Data R2DBC automatically determines INSERT vs UPDATE
 * based on whether accountNumber is null (INSERT) or not (UPDATE)
 * <p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("account")
public class AccountEntity {

    @Id
    @Column("account_number")
    private Long accountNumber;

    @Column("customer_id")
    private UUID customerId;

    @Column("customer_name")
    private String customerName;  // Denormalized for performance

    @Column("account_type")
    private String accountType;  // Stored as String, mapped from enum

    @Column("balance")
    private BigDecimal balance;

    @Column("state")
    private Boolean state;

    @Version
    @Column("version")
    private Integer version;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
