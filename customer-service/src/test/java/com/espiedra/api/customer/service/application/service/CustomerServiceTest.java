package com.espiedra.api.customer.service.application.service;

import com.espiedra.api.customer.service.application.dto.CustomerCreationResult;
import com.espiedra.api.customer.service.application.ports.output.CustomerRepositoryPort;
import com.espiedra.api.customer.service.application.ports.output.EventPublisherPort;
import com.espiedra.api.customer.service.application.ports.output.JwtTokenPort;
import com.espiedra.api.customer.service.application.ports.output.PasswordEncoderPort;
import com.espiedra.api.customer.service.domain.exception.CustomerAlreadyExistsException;
import com.espiedra.api.customer.service.domain.exception.CustomerNotFoundException;
import com.espiedra.api.customer.service.domain.exception.InvalidPasswordException;
import com.espiedra.api.customer.service.domain.model.Customer;
import com.espiedra.api.customer.service.domain.model.GenderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS: CustomerService
 * <p>
 * TESTING STRATEGY:
 * - Test customer CRUD operations
 * - Test business rule validations
 * - Test password operations
 * - Test exception scenarios
 * - Test state transitions (activate/deactivate)
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
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepository;

    @Mock
    private EventPublisherPort eventPublisher;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private JwtTokenPort jwtToken;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;
    private String rawPassword;
    private String encodedPassword;
    private String correlationId;

    @BeforeEach
    void setUp() {
        // Arrange: Create test customer
        testCustomer = Customer.create(
                "Juan Pérez",
                GenderType.M,
                30,
                "1234567890",
                "Av. Principal 123",
                "0999999999",
                "encodedPassword123"
        );
        testCustomer.setCustomerId(UUID.randomUUID());
        testCustomer.setVersion(1L);
        testCustomer.setCreatedAt(LocalDateTime.now());

        rawPassword = "securePassword123";
        encodedPassword = "encodedPassword123";
        correlationId = UUID.randomUUID().toString();
    }

    /**
     * TEST: Create Customer - Success
     * <p>
     * BUSINESS RULE: Customer must not already exist by identification
     * EXPECTED: Customer should be created with encoded password and JWT token
     */
    @Test
    @DisplayName("Should create customer successfully with encoded password and JWT token")
    void testCreateCustomer_Success() {
        // Given: Customer does not exist
        when(customerRepository.existsByIdentification(testCustomer.getIdentification()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(testCustomer));
        when(jwtToken.generateToken(any(UUID.class), anyString())).thenReturn("jwt-token-12345");
        when(eventPublisher.publishCustomerEvent(any())).thenReturn(Mono.empty());

        // When: Create customer
        Mono<CustomerCreationResult> result = customerService.createCustomer(
                testCustomer, rawPassword, correlationId
        );

        // Then: Customer should be created successfully
        StepVerifier.create(result)
                .assertNext(creationResult -> {
                    assertThat(creationResult).isNotNull();
                    assertThat(creationResult.customer()).isNotNull();
                    assertThat(creationResult.customer().getName()).isEqualTo("Juan Pérez");
                    assertThat(creationResult.customer().getIdentification()).isEqualTo("1234567890");
                    assertThat(creationResult.jwtToken()).isEqualTo("jwt-token-12345");
                })
                .verifyComplete();

        // Verify interactions
        verify(customerRepository, times(1)).existsByIdentification("1234567890");
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(jwtToken, times(1)).generateToken(any(UUID.class), eq("1234567890"));
        verify(eventPublisher, times(1)).publishCustomerEvent(any());
    }

    /**
     * TEST: Create Customer - Already Exists
     * <p>
     * BUSINESS RULE: Identification must be unique
     * EXPECTED: CustomerAlreadyExistsException should be thrown
     */
    @Test
    @DisplayName("Should throw CustomerAlreadyExistsException when identification already exists")
    void testCreateCustomer_AlreadyExists() {
        // Given: Customer already exists
        when(customerRepository.existsByIdentification(testCustomer.getIdentification()))
                .thenReturn(Mono.just(true));

        // When: Attempt to create customer with existing identification
        Mono<CustomerCreationResult> result = customerService.createCustomer(
                testCustomer, rawPassword, correlationId
        );

        // Then: Should throw CustomerAlreadyExistsException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomerAlreadyExistsException &&
                                throwable.getMessage().contains("1234567890"))
                .verify();

        // Verify customer was NOT saved
        verify(customerRepository, never()).save(any(Customer.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtToken, never()).generateToken(any(), anyString());
    }

    /**
     * TEST: Get Customer by ID - Success
     * <p>
     * BUSINESS RULE: Customer must exist
     * EXPECTED: Customer should be returned
     */
    @Test
    @DisplayName("Should retrieve customer by ID successfully")
    void testGetCustomerById_Success() {
        // Given: Customer exists
        UUID customerId = testCustomer.getCustomerId();
        when(customerRepository.findById(customerId)).thenReturn(Mono.just(testCustomer));

        // When: Get customer by ID
        Mono<Customer> result = customerService.getCustomerById(customerId);

        // Then: Customer should be returned
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer).isNotNull();
                    assertThat(customer.getCustomerId()).isEqualTo(customerId);
                    assertThat(customer.getName()).isEqualTo("Juan Pérez");
                    assertThat(customer.getIdentification()).isEqualTo("1234567890");
                })
                .verifyComplete();

        verify(customerRepository, times(1)).findById(customerId);
    }

    /**
     * TEST: Get Customer by ID - Not Found
     * <p>
     * BUSINESS RULE: Customer must exist
     * EXPECTED: CustomerNotFoundException should be thrown
     */
    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
    void testGetCustomerById_NotFound() {
        // Given: Customer does not exist
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Mono.empty());

        // When: Attempt to get non-existent customer
        Mono<Customer> result = customerService.getCustomerById(customerId);

        // Then: Should throw CustomerNotFoundException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomerNotFoundException &&
                                throwable.getMessage().contains(customerId.toString()))
                .verify();

        verify(customerRepository, times(1)).findById(customerId);
    }

    /**
     * TEST: Update Customer - Success
     * <p>
     * BUSINESS RULE: Version must match (optimistic locking)
     * EXPECTED: Customer should be updated successfully
     */
    @Test
    @DisplayName("Should update customer successfully with valid version")
    void testUpdateCustomer_Success() {
        // Given: Customer exists with matching version
        UUID customerId = testCustomer.getCustomerId();
        Long expectedVersion = testCustomer.getVersion();

        Customer updatedData = testCustomer.toBuilder()
                .name("Juan Pérez Updated")
                .address("New Address 456")
                .phone("0988888888")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Mono.just(testCustomer));
        when(customerRepository.update(any(Customer.class))).thenReturn(Mono.just(updatedData));
        when(eventPublisher.publishCustomerEvent(any())).thenReturn(Mono.empty());

        // When: Update customer
        Mono<Customer> result = customerService.updateCustomer(
                customerId, updatedData, expectedVersion, correlationId
        );

        // Then: Customer should be updated
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer).isNotNull();
                    assertThat(customer.getName()).isEqualTo("Juan Pérez Updated");
                    assertThat(customer.getAddress()).isEqualTo("New Address 456");
                })
                .verifyComplete();

        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).update(any(Customer.class));
        verify(eventPublisher, times(1)).publishCustomerEvent(any());
    }

    /**
     * TEST: Update Password - Success
     * <p>
     * BUSINESS RULE: Current password must be correct
     * EXPECTED: Password should be updated with new encoded value
     */
    @Test
    @DisplayName("Should update password successfully when current password is correct")
    void testUpdatePassword_Success() {
        // Given: Customer exists with correct current password
        UUID customerId = testCustomer.getCustomerId();
        Long expectedVersion = testCustomer.getVersion();
        String currentPassword = "oldPassword";
        String newPassword = "newPassword123";
        String newEncodedPassword = "encodedNewPassword";

        when(customerRepository.findById(customerId)).thenReturn(Mono.just(testCustomer));
        when(passwordEncoder.matches(currentPassword, testCustomer.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
        when(customerRepository.update(any(Customer.class))).thenReturn(Mono.just(testCustomer));

        // When: Update password
        Mono<Customer> result = customerService.updatePassword(
                customerId, currentPassword, newPassword, expectedVersion, correlationId
        );

        // Then: Password should be updated
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer).isNotNull();
                    assertThat(customer.getCustomerId()).isEqualTo(customerId);
                })
                .verifyComplete();

        verify(passwordEncoder, times(1)).matches(currentPassword, testCustomer.getPassword());
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(customerRepository, times(1)).update(any(Customer.class));
    }

    /**
     * TEST: Update Password - Invalid Current Password
     * <p>
     * BUSINESS RULE: Current password must match
     * EXPECTED: InvalidPasswordException should be thrown
     */
    @Test
    @DisplayName("Should throw InvalidPasswordException when current password is incorrect")
    void testUpdatePassword_InvalidCurrentPassword() {
        // Given: Customer exists but current password is incorrect
        UUID customerId = testCustomer.getCustomerId();
        Long expectedVersion = testCustomer.getVersion();
        String wrongCurrentPassword = "wrongPassword";
        String newPassword = "newPassword123";

        when(customerRepository.findById(customerId)).thenReturn(Mono.just(testCustomer));
        when(passwordEncoder.matches(wrongCurrentPassword, testCustomer.getPassword())).thenReturn(false);

        // When: Attempt to update with wrong current password
        Mono<Customer> result = customerService.updatePassword(
                customerId, wrongCurrentPassword, newPassword, expectedVersion, correlationId
        );

        // Then: Should throw InvalidPasswordException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof InvalidPasswordException &&
                                throwable.getMessage().contains("incorrect"))
                .verify();

        // Verify password was NOT updated
        verify(passwordEncoder, never()).encode(anyString());
        verify(customerRepository, never()).update(any(Customer.class));
    }

    /**
     * TEST: Activate Customer - Success
     * <p>
     * BUSINESS RULE: Customer state should change to active
     * EXPECTED: Customer should be activated (state = true)
     */
    @Test
    @DisplayName("Should activate customer successfully")
    void testActivateCustomer_Success() {
        // Given: Customer exists and is inactive
        UUID customerId = testCustomer.getCustomerId();
        Long expectedVersion = testCustomer.getVersion();
        testCustomer.setState(false); // Initially inactive

        Customer activatedCustomer = testCustomer.toBuilder()
                .state(true)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Mono.just(testCustomer));
        when(customerRepository.update(any(Customer.class))).thenReturn(Mono.just(activatedCustomer));
        when(eventPublisher.publishCustomerEvent(any())).thenReturn(Mono.empty());

        // When: Activate customer
        Mono<Customer> result = customerService.activateCustomer(
                customerId, expectedVersion, correlationId
        );

        // Then: Customer should be activated
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer).isNotNull();
                    assertThat(customer.getState()).isTrue();
                })
                .verifyComplete();

        verify(customerRepository, times(1)).update(any(Customer.class));
        verify(eventPublisher, times(1)).publishCustomerEvent(any());
    }

    /**
     * TEST: Deactivate Customer - Success
     * <p>
     * BUSINESS RULE: Customer state should change to inactive
     * EXPECTED: Customer should be deactivated (state = false)
     */
    @Test
    @DisplayName("Should deactivate customer successfully")
    void testDeactivateCustomer_Success() {
        // Given: Customer exists and is active
        UUID customerId = testCustomer.getCustomerId();
        Long expectedVersion = testCustomer.getVersion();
        testCustomer.setState(true); // Initially active

        Customer deactivatedCustomer = testCustomer.toBuilder()
                .state(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Mono.just(testCustomer));
        when(customerRepository.update(any(Customer.class))).thenReturn(Mono.just(deactivatedCustomer));
        when(eventPublisher.publishCustomerEvent(any())).thenReturn(Mono.empty());

        // When: Deactivate customer
        Mono<Customer> result = customerService.deactivateCustomer(
                customerId, expectedVersion, correlationId
        );

        // Then: Customer should be deactivated
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer).isNotNull();
                    assertThat(customer.getState()).isFalse();
                })
                .verifyComplete();

        verify(customerRepository, times(1)).update(any(Customer.class));
        verify(eventPublisher, times(1)).publishCustomerEvent(any());
    }

    /**
     * TEST: Get All Customers - Success
     * <p>
     * BUSINESS RULE: Should return paginated customers
     * EXPECTED: All customers in page should be returned
     */
    @Test
    @DisplayName("Should retrieve all customers with pagination")
    void testGetAllCustomers_Success() {
        // Given: Multiple customers exist
        Customer customer2 = Customer.create(
                "Maria García",
                GenderType.F,
                25,
                "0987654321",
                "Calle 456",
                "0988888888",
                "encodedPass"
        );
        customer2.setCustomerId(UUID.randomUUID());

        when(customerRepository.findAll(0, 10))
                .thenReturn(Flux.just(testCustomer, customer2));

        // When: Get all customers
        Flux<Customer> result = customerService.getAllCustomers(0, 10);

        // Then: Should return all customers
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer.getName()).isEqualTo("Juan Pérez");
                })
                .assertNext(customer -> {
                    assertThat(customer.getName()).isEqualTo("Maria García");
                })
                .verifyComplete();

        verify(customerRepository, times(1)).findAll(0, 10);
    }

    /**
     * TEST: Delete Customer - Success
     * <p>
     * BUSINESS RULE: Customer must exist to be deleted
     * EXPECTED: Customer should be deleted and event published
     */
    @Test
    @DisplayName("Should delete customer successfully")
    void testDeleteCustomer_Success() {
        // Given: Customer exists
        UUID customerId = testCustomer.getCustomerId();

        when(customerRepository.findById(customerId)).thenReturn(Mono.just(testCustomer));
        when(customerRepository.deleteById(customerId)).thenReturn(Mono.empty());
        when(eventPublisher.publishCustomerEvent(any())).thenReturn(Mono.empty());

        // When: Delete customer
        Mono<Void> result = customerService.deleteCustomer(customerId, correlationId);

        // Then: Customer should be deleted
        StepVerifier.create(result)
                .verifyComplete();

        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).deleteById(customerId);
        verify(eventPublisher, times(1)).publishCustomerEvent(any());
    }
}
