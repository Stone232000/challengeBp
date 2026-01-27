package com.espiedra.api.account.service.infrastructure.adapter.input.rest.mapper;

import com.espiedra.api.account.service.application.dto.AccountStatementReport;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.AccountStatementResponse;
import com.espiedra.api.account.service.infrastructure.input.adapter.rest.account.service.models.MovementsSummaryResponse;
import org.mapstruct.Mapper;

/**
 * API MAPPER: ReportApiMapper
 * <p>
 * DESIGN PATTERNS:
 * - Mapper Pattern: Maps between API and Application layers
 * - Anti-Corruption Layer: Protects application from API changes
 * <p>
 * MAPPING STRATEGY:
 * - Uses MapStruct for compile-time generation
 * - Constructor injection for Spring beans
 * - Custom default methods for String <-> OffsetDateTime conversion
 *
 */
@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface ReportApiMapper {

    /**
     * Convert AccountStatementReport (Application DTO) to AccountStatementResponse (OpenAPI)
     * <p>
     * MapStruct will automatically use DateTimeMapper for String <-> OffsetDateTime conversion
     */
    AccountStatementResponse toResponse(AccountStatementReport report);

}
