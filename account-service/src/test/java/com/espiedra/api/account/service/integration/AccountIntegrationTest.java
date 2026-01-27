package com.espiedra.api.account.service.integration;

import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.entity.AccountEntity;
import com.espiedra.api.account.service.infrastructure.adapter.output.persistence.repository.AccountR2dbcRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INTEGRATION TEST: Account database operations with Testcontainers

@DataR2dbcTest
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("Account Integration Test")
class AccountIntegrationTest {

    private final AccountR2dbcRepository accountRepository;
    private Long testAccountNumber;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("schema.sql");

    AccountIntegrationTest(AccountR2dbcRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            postgres.getJdbcUrl().replace("jdbc:", "r2dbc:"));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }



    @AfterEach
    void cleanup() {
        if (testAccountNumber != null) {
            accountRepository.deleteById(testAccountNumber).block();
        }
    }

    @Test
    @DisplayName("Should save and retrieve account from database")
    void testSaveAndRetrieveAccount() {
        // Given: Create account
        AccountEntity account = new AccountEntity();
        account.setCustomerId(UUID.randomUUID());
        account.setCustomerName("Test Customer");
        account.setAccountType("AHORRO");
        account.setBalance(new BigDecimal("1000.00"));
        account.setState(true);

        // When: Save account
        AccountEntity savedAccount = accountRepository.save(account).block();

        // Then: Verify account was saved
        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isNotNull();
        testAccountNumber = savedAccount.getAccountNumber();
        assertThat(savedAccount.getCustomerName()).isEqualTo("Test Customer");
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(savedAccount.getState()).isTrue();

        // When: Retrieve account
        AccountEntity retrievedAccount = accountRepository.findById(testAccountNumber).block();

        // Then: Verify account was retrieved correctly
        assertThat(retrievedAccount).isNotNull();
        assertThat(retrievedAccount.getAccountNumber()).isEqualTo(testAccountNumber);
        assertThat(retrievedAccount.getCustomerName()).isEqualTo("Test Customer");
        assertThat(retrievedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
 */