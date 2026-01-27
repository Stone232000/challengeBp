package com.espiedra.api.account.service.infrastructure.adapter.input.messaging.kafka.consumer;

import com.espiedra.api.account.service.application.port.input.DeleteAccountUseCase;
import com.espiedra.api.account.service.application.service.AccountCreationService;
import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.domain.model.AccountType;
import com.espiedra.api.account.service.infrastructure.adapter.input.messaging.kafka.dto.CustomerEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * KAFKA CONSUMER: Customer Event Consumer
 * <p>
 * Input Adapter (Driving) in Hexagonal Architecture.
 * Consumes customer events from Kafka topic and performs async operations.
 * <p>
 * DESIGN PATTERNS:
 * - Adapter Pattern: Adapts Kafka messages to domain use cases
 * - Consumer Pattern: Listens to event stream
 * - Event-Driven Architecture: Reacts to customer events
 * <p>
 * RESPONSIBILITIES:
 * - Listen to banking.customer.events topic
 * - Deserialize Kafka messages to DTOs
 * - Handle customer.created: Create default AHORROS account
 * - Handle customer.deleted: Delete all accounts and movements in cascade
 * <p>
 * EVENT TYPES HANDLED:
 * - customer.created: New customer → Create default AHORROS account with 0 initial balance
 * - customer.updated: Customer modified → No action (only for audit logs)
 * - customer.deleted: Customer removed → Delete all accounts and movements
 * <p>
 * RESILIENCE:
 * - Error handling with logging
 * - Kafka will retry on failure (based on configuration)
 * - Non-blocking reactive processing
 * <p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventConsumer {

    private final AccountCreationService accountCreationService;
    private final DeleteAccountUseCase deleteAccountUseCase;

    /**
     * Consume customer events from Kafka topic
     * <p>
     * Listens to 'banking.customer.events' topic and processes incoming events.
     *
     * @param event Customer event DTO
     * @param partition Kafka partition number
     * @param offset Message offset
     */
    @KafkaListener(
            topics = "${spring.kafka.consumer.topics.customer-events:banking.customer.events}",
            groupId = "${spring.kafka.consumer.group-id:account-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCustomerEvent(
            @Payload CustomerEventDTO event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received customer event: eventId={}, eventType={}, customerId={}, partition={}, offset={}",
                event.getEventId(), event.getEventType(), event.getCustomerId(), partition, offset);

        try {
            processEvent(event)
                    .doOnSuccess(v -> log.info("Customer event processed successfully: eventId={}, eventType={}, customerId={}",
                            event.getEventId(), event.getEventType(), event.getCustomerId()))
                    .doOnError(error -> log.error("Error processing customer event: eventId={}, eventType={}, customerId={}, error={}",
                            event.getEventId(), event.getEventType(), event.getCustomerId(), error.getMessage(), error))
                    .subscribe();

        } catch (Exception e) {
            log.error("Unexpected error consuming customer event: eventId={}, customerId={}, error={}",
                    event.getEventId(), event.getCustomerId(), e.getMessage(), e);
        }
    }

    /**
     * Process customer event based on event type
     *
     * @param event Customer event
     * @return Mono<Void> Completion signal
     */
    private Mono<Void> processEvent(CustomerEventDTO event) {
        String eventType = event.getEventType();

        return switch (eventType) {
            case "customer.created" -> handleCustomerCreated(event);
            case "customer.updated" -> handleCustomerUpdated(event);
            case "customer.deleted" -> handleCustomerDeleted(event);
            default -> {
                log.warn("Unknown event type received: eventType={}, eventId={}, customerId={}", eventType, event.getEventId(), event.getCustomerId());
                yield Mono.empty();
            }
        };
    }

    /**
     * Handle customer.created event
     * <p>
     * Creates a default AHORROS (Savings) account with 0 initial balance.
     * <p>
     * BUSINESS RULE:
     * - When a customer is created, automatically create a savings account
     * - Initial balance: 0
     * - Account type: AHORRO (Savings)
     * - State: Active (true)
     *
     * @param event Customer event
     * @return Mono<Void> Completion signal
     */
    private Mono<Void> handleCustomerCreated(CustomerEventDTO event) {
        log.info("Processing customer.created: Creating default AHORRO account for customerId={}, customerName={}", event.getCustomerId(), event.getName());

        // Create default AHORRO account
        Account defaultAccount = Account.builder()
                .customerId(event.getCustomerId())
                .customerName(event.getName())
                .accountType(AccountType.AHORRO)
                .initialBalance(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .state(true)
                .build();

        return accountCreationService.createAccountWithoutCustomerValidation(defaultAccount)
                .doOnSuccess(createdAccount -> log.info(
                        "Default AHORRO account created successfully: accountNumber={}, customerId={}",
                        createdAccount.getAccountNumber(), event.getCustomerId()))
                .doOnError(error -> log.error("Failed to create default account for customerId={}: {}", event.getCustomerId(), error.getMessage()))
                .then();
    }

    /**
     * Handle customer.updated event
     *
     * No action required - this is only for audit/logging purposes.
     * Account Service doesn't need to react to customer updates.
     *
     * @param event Customer event
     * @return Mono<Void> Completion signal
     */
    private Mono<Void> handleCustomerUpdated(CustomerEventDTO event) {
        log.info("Processing customer.updated: customerId={} - No action required (audit only)", event.getCustomerId());
        return Mono.empty();
    }

    /**
     * Handle customer.deleted event
     * <p>
     * Deletes all accounts and movements for the customer in cascade.
     * <p>
     * BUSINESS RULE:
     * - When a customer is deleted, all their accounts must be deleted
     * - All movements for those accounts must also be deleted (cascade)
     * - This maintains referential integrity across microservices
     *
     * @param event Customer event
     * @return Mono<Void> Completion signal
     */
    private Mono<Void> handleCustomerDeleted(CustomerEventDTO event) {
        log.info("Processing customer.deleted: Deleting all accounts and movements for customerId={}", event.getCustomerId());

        return deleteAccountUseCase.deleteAccountsByCustomerId(event.getCustomerId())
                .doOnSuccess(count -> log.info("Deleted {} accounts (with movements) for customerId={}", count, event.getCustomerId()))
                .doOnError(error -> log.error("Failed to delete accounts for customerId={}: {}", event.getCustomerId(), error.getMessage()))
                .then();
    }
}
