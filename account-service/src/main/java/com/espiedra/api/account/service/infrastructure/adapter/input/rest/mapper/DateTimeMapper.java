package com.espiedra.api.account.service.infrastructure.adapter.input.rest.mapper;

import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * MAPPER: DateTimeMapper
 * <p>
 * Provides conversion methods for date/time types used by MapStruct.
 * This is a utility mapper that can be referenced by other mappers.
 * <p>
 * DESIGN PATTERN: Strategy Pattern
 * Uses multiple parsing strategies in sequence until one succeeds.
 * <p>
 * CLEAN CODE PRINCIPLE:
 * - Avoids nested try-catch blocks
 * - Each parsing strategy is encapsulated
 * - Easy to add new parsing strategies
 */
@Mapper(componentModel = "spring")
public class DateTimeMapper {

    /**
     * List of parsing strategies in order of preference.
     * Each strategy attempts to parse the date string.
     * First successful parse wins.
     */
    private static final List<Function<String, Optional<OffsetDateTime>>> PARSING_STRATEGIES = List.of(
            DateTimeMapper::parseIsoOffsetDateTime,
            DateTimeMapper::parseOffsetDateTimeRelaxed,
            DateTimeMapper::parseLocalDateTimeWithSystemOffset
    );

    /**
     * Convert String (ISO 8601) to OffsetDateTime
     * MapStruct will automatically discover and use this method
     * <p>
     * Handles various ISO-8601 formats including edge cases with inconsistent nanosecond precision
     * and LocalDateTime without timezone offset
     * <p>
     * STRATEGY PATTERN: Tries multiple parsing strategies in sequence
     *
     * @param dateString Date string to parse
     * @return OffsetDateTime or null if input is null/empty
     * @throws IllegalArgumentException if no strategy can parse the date
     */
    public OffsetDateTime map(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        return PARSING_STRATEGIES.stream()
                .map(strategy -> strategy.apply(dateString))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot parse date string: '" + dateString + "'. " +
                        "Supported formats: ISO_OFFSET_DATE_TIME, ISO_DATE_TIME, ISO_LOCAL_DATE_TIME"
                ));
    }

    /**
     * Convert OffsetDateTime to String (ISO 8601)
     *
     * @param dateTime OffsetDateTime to format
     * @return ISO 8601 formatted string or null if input is null
     */
    public String map(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * STRATEGY 1: Parse using ISO_OFFSET_DATE_TIME formatter
     * Example: "2025-11-10T22:07:45.123+00:00"
     *
     * @param dateString Date string to parse
     * @return Optional containing OffsetDateTime if successful, empty otherwise
     */
    private static Optional<OffsetDateTime> parseIsoOffsetDateTime(String dateString) {
        try {
            return Optional.of(OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * STRATEGY 2: Parse using relaxed OffsetDateTime parser
     * Handles variations in ISO format
     *
     * @param dateString Date string to parse
     * @return Optional containing OffsetDateTime if successful, empty otherwise
     */
    private static Optional<OffsetDateTime> parseOffsetDateTimeRelaxed(String dateString) {
        try {
            return Optional.of(OffsetDateTime.parse(dateString));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * STRATEGY 3: Parse as LocalDateTime and convert to OffsetDateTime
     * Uses system default zone offset (e.g., -05:00 for America/Bogota)
     * Example: "2025-11-10T22:07:45.123"
     *
     * @param dateString Date string to parse
     * @return Optional containing OffsetDateTime if successful, empty otherwise
     */
    private static Optional<OffsetDateTime> parseLocalDateTimeWithSystemOffset(String dateString) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            ZoneOffset offset = ZoneOffset.systemDefault().getRules().getOffset(localDateTime);
            return Optional.of(localDateTime.atOffset(offset));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
