package com.espiedra.api.account.service.infrastructure.adapter.input.rest.mapper;

import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.AccountCreateRequest;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * MAPPER: AccountApiMapper (MapStruct)
 * <p>
 * DESIGN PATTERNS:
 * - Mapper Pattern: Translates between layers
 * - Adapter Pattern: Adapts API contracts to domain
 * - Anti-Corruption Layer: Protects domain from API changes
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only maps Account API models
 * - Dependency Inversion: API depends on domain, not vice versa
 * <p>
 * TYPE CONVERSIONS:
 * - Double (OpenAPI) <-> BigDecimal (Domain)
 * - OffsetDateTime (OpenAPI) <-> LocalDateTime (Domain)
 * - AccountType enum is same in both layers
 * <p>
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR
)
public interface AccountApiMapper {

    /**
     * Convert AccountCreateRequest (API) to Account (Domain)
     * <p>
     * Used when creating a new account from REST request.
     * Sets initialBalance = balance for new accounts.
     *
     * @param request OpenAPI request model
     * @return Account domain model
     */
    @Mapping(target = "balance", source = "initialBalance")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    Account toDomain(AccountCreateRequest request);

    /**
     * Convert Account (Domain) to AccountResponse (API)
     * <p>
     * Used when returning account data to REST client.
     * customerName is not mapped here - must be set separately by enriching with Customer data.
     *
     * @param account Domain model
     * @return AccountResponse OpenAPI model
     */
    @Mapping(target = "customerName", ignore = true)
    AccountResponse toResponse(Account account);

    /**
     * Convert Double to BigDecimal
     * Handles null values and precision conversion
     */
    default BigDecimal doubleToBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    /**
     * Convert BigDecimal to Double
     * Handles null values and precision conversion
     */
    default Double bigDecimalToDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Convert LocalDateTime to OffsetDateTime
     * Assumes UTC timezone
     */
    default OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

}
