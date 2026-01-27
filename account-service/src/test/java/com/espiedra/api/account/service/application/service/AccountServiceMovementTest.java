package com.espiedra.api.account.service.application.service;

import com.espiedra.api.account.service.application.port.output.AccountRepositoryPort;
import com.espiedra.api.account.service.application.port.output.CustomerServiceClientPort;
import com.espiedra.api.account.service.application.port.output.EventPublisherPort;
import com.espiedra.api.account.service.application.port.output.MovementRepositoryPort;
import com.espiedra.api.account.service.domain.exception.AccountNotActiveException;
import com.espiedra.api.account.service.domain.exception.AccountNotFoundException;
import com.espiedra.api.account.service.domain.exception.InsufficientBalanceException;
import com.espiedra.api.account.service.domain.model.Account;
import com.espiedra.api.account.service.domain.model.AccountType;
import com.espiedra.api.account.service.domain.model.Movement;
import com.espiedra.api.account.service.domain.model.MovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS: AccountService - Movement Operations
 * <p>
 * TESTING STRATEGY:
 * - Test movement creation (CREDITO/DEBITO)
 * - Test business rule validations
 * - Test idempotency checks
 * - Test balance updates
 * - Test exception scenarios
 * <p>
 * TESTING PATTERNS:
 * - Arrange-Act-Assert (AAA)
 * - Given-When-Then (BDD style)
 * - Reactive testing with StepVerifier
 * - Mocking with Mockito
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Each test validates one behavior
 * - Dependency Inversion: Tests depend on interfaces (ports)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService - Movement Operations Unit Tests")
class AccountServiceMovementTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private MovementRepositoryPort movementRepositoryPort;

    @Mock
    private CustomerServiceClientPort customerServiceClientPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private MovementProcessingService movementProcessingService;

    private Account testAccount;
    private Movement testMovement;

    @BeforeEach
    void setUp() {
        // Arrange: Create test account with initial balance
        testAccount = new Account();
        testAccount.setAccountNumber(1001L);
        testAccount.setCustomerId(UUID.randomUUID());
        testAccount.setCustomerName("Juan Pérez");
        testAccount.setAccountType(AccountType.AHORRO);
        testAccount.setInitialBalance(new BigDecimal("1000.00"));
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setState(true);

        // Arrange: Create test movement
        testMovement = new Movement();
        testMovement.setMovementId(UUID.randomUUID());
        testMovement.setAccountNumber(1001L);
        testMovement.setMovementType(MovementType.CREDITO);
        testMovement.setAmount(new BigDecimal("500.00"));
        testMovement.setTransactionId("TXN-12345");
        testMovement.setIdempotencyKey(UUID.randomUUID());
        testMovement.setCreatedAt(LocalDateTime.now());
    }

    /**
     * TEST: Create Movement - Successful Credit (Deposit)
     * <p>
     * BUSINESS RULE: Credit increases account balance
     * EXPECTED: Balance should increase by credit amount
     */
    @Test
    @DisplayName("Should create CREDITO movement and increase balance successfully")
    void testCreateMovement_Credit_Success() {
        // Given: Account exists, is active, and transaction is unique
        when(movementRepositoryPort.existsByTransactionId(anyString())).thenReturn(Mono.just(false));
        when(movementRepositoryPort.findByIdempotencyKey(any(UUID.class))).thenReturn(Mono.empty());
        when(accountRepositoryPort.findByAccountNumber(1001L)).thenReturn(Mono.just(testAccount));

        // Mock movement save with balance update
        testMovement.setBalanceBefore(new BigDecimal("1000.00"));
        testMovement.setBalanceAfter(new BigDecimal("1500.00"));
        when(movementRepositoryPort.save(any(Movement.class))).thenReturn(Mono.just(testMovement));

        // Mock event publisher
        when(eventPublisherPort.publish(anyString(), anyString(), any())).thenReturn(Mono.empty());

        // When: Create credit movement
        Mono<Movement> result = movementProcessingService.createMovement(testMovement);

        // Then: Movement should be created with correct balance tracking
        StepVerifier.create(result)
                .assertNext(movement -> {
                    assertThat(movement).isNotNull();
                    assertThat(movement.getMovementType()).isEqualTo(MovementType.CREDITO);
                    assertThat(movement.getBalanceBefore()).isEqualByComparingTo("1000.00");
                    assertThat(movement.getBalanceAfter()).isEqualByComparingTo("1500.00");
                    assertThat(movement.getAmount()).isEqualByComparingTo("500.00");
                })
                .verifyComplete();

        // Verify interactions
        verify(movementRepositoryPort, times(1)).existsByTransactionId(testMovement.getTransactionId());
        verify(accountRepositoryPort, times(1)).findByAccountNumber(1001L);
        verify(movementRepositoryPort, times(1)).save(any(Movement.class));
    }

    /**
     * TEST: Create Movement - Successful Debit (Withdrawal)
     * <p>
     * BUSINESS RULE: Debit decreases account balance
     * BUSINESS RULE: Balance cannot go below -10,000 (overdraft limit)
     * EXPECTED: Balance should decrease by debit amount
     */
    @Test
    @DisplayName("Should create DEBITO movement and decrease balance successfully")
    void testCreateMovement_Debit_Success() {
        // Given: Debit movement with sufficient balance
        testMovement.setMovementType(MovementType.DEBITO);
        testMovement.setAmount(new BigDecimal("300.00"));

        when(movementRepositoryPort.existsByTransactionId(anyString())).thenReturn(Mono.just(false));
        when(movementRepositoryPort.findByIdempotencyKey(any(UUID.class))).thenReturn(Mono.empty());
        when(accountRepositoryPort.findByAccountNumber(1001L)).thenReturn(Mono.just(testAccount));

        // Mock movement save with balance update
        testMovement.setBalanceBefore(new BigDecimal("1000.00"));
        testMovement.setBalanceAfter(new BigDecimal("700.00"));
        when(movementRepositoryPort.save(any(Movement.class))).thenReturn(Mono.just(testMovement));

        // Mock event publisher
        when(eventPublisherPort.publish(anyString(), anyString(), any())).thenReturn(Mono.empty());

        // When: Create debit movement
        Mono<Movement> result = movementProcessingService.createMovement(testMovement);

        // Then: Movement should be created with decreased balance
        StepVerifier.create(result)
                .assertNext(movement -> {
                    assertThat(movement).isNotNull();
                    assertThat(movement.getMovementType()).isEqualTo(MovementType.DEBITO);
                    assertThat(movement.getBalanceBefore()).isEqualByComparingTo("1000.00");
                    assertThat(movement.getBalanceAfter()).isEqualByComparingTo("700.00");
                    assertThat(movement.getAmount()).isEqualByComparingTo("300.00");
                })
                .verifyComplete();

        verify(movementRepositoryPort, times(1)).save(any(Movement.class));
    }

    /**
     * TEST: Create Movement - Insufficient Balance
     * <p>
     * BUSINESS RULE: Cannot withdraw if balance would go below -10,000
     * EXPECTED: InsufficientBalanceException should be thrown
     */
    @Test
    @DisplayName("Should throw InsufficientBalanceException when withdrawal exceeds overdraft limit")
    void testCreateMovement_InsufficientBalance() {
        // Given: Debit movement exceeding available balance + overdraft
        testMovement.setMovementType(MovementType.DEBITO);
        testMovement.setAmount(new BigDecimal("12000.00")); // Exceeds 1000 + 10000 overdraft

        when(movementRepositoryPort.existsByTransactionId(anyString())).thenReturn(Mono.just(false));
        when(movementRepositoryPort.findByIdempotencyKey(any(UUID.class))).thenReturn(Mono.empty());
        when(accountRepositoryPort.findByAccountNumber(1001L)).thenReturn(Mono.just(testAccount));

        // When: Attempt to create debit movement with insufficient balance
        Mono<Movement> result = movementProcessingService.createMovement(testMovement);

        // Then: Should throw InsufficientBalanceException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof InsufficientBalanceException &&
                                throwable.getMessage().contains("Insufficient balance"))
                .verify();

        // Verify movement was NOT saved
        verify(movementRepositoryPort, never()).save(any(Movement.class));
    }

    /**
     * TEST: Create Movement - Account Not Found
     * <p>
     * BUSINESS RULE: Account must exist
     * EXPECTED: AccountNotFoundException should be thrown
     */
    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void testCreateMovement_AccountNotFound() {
        // Given: Account does not exist
        when(movementRepositoryPort.existsByTransactionId(anyString())).thenReturn(Mono.just(false));
        when(movementRepositoryPort.findByIdempotencyKey(any(UUID.class))).thenReturn(Mono.empty());
        when(accountRepositoryPort.findByAccountNumber(1001L)).thenReturn(Mono.empty());

        // When: Attempt to create movement for non-existent account
        Mono<Movement> result = movementProcessingService.createMovement(testMovement);

        // Then: Should throw AccountNotFoundException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof AccountNotFoundException &&
                                throwable.getMessage().contains("1001"))
                .verify();

        verify(movementRepositoryPort, never()).save(any(Movement.class));
    }

    /**
     * TEST: Create Movement - Account Not Active
     * <p>
     * BUSINESS RULE: Movements can only be created for active accounts
     * EXPECTED: AccountNotActiveException should be thrown
     */
    @Test
    @DisplayName("Should throw AccountNotActiveException when account is inactive")
    void testCreateMovement_AccountNotActive() {
        // Given: Account exists but is inactive
        testAccount.setState(false);

        when(movementRepositoryPort.existsByTransactionId(anyString())).thenReturn(Mono.just(false));
        when(movementRepositoryPort.findByIdempotencyKey(any(UUID.class))).thenReturn(Mono.empty());
        when(accountRepositoryPort.findByAccountNumber(1001L)).thenReturn(Mono.just(testAccount));

        // When: Attempt to create movement for inactive account
        Mono<Movement> result = movementProcessingService.createMovement(testMovement);

        // Then: Should throw AccountNotActiveException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof AccountNotActiveException &&
                                throwable.getMessage().contains("not active"))
                .verify();

        verify(movementRepositoryPort, never()).save(any(Movement.class));
    }

}
