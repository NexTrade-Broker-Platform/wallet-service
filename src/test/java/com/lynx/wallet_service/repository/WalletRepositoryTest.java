package com.lynx.wallet_service.repository;

import com.lynx.wallet_service.wallet.entity.Wallet;
import com.lynx.wallet_service.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class WalletRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("db_wallet_test")
            .withUsername("postgres")
            .withPassword("yourpassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://dummy-issuer");
    }

    @Autowired
    private WalletRepository walletRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        userId = UUID.randomUUID();
    }

    @Test
    void save_shouldPersistWallet() {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .availableBalance(new BigDecimal("1000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        Wallet saved = walletRepository.save(wallet);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void findByUserIdAndCurrencyAndIsActiveTrue_shouldReturnWallet() {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .availableBalance(new BigDecimal("500.00"))
                .reservedBalance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);

        Optional<Wallet> found = walletRepository
                .findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD");

        assertThat(found).isPresent();
        assertThat(found.get().getCurrency()).isEqualTo("USD");
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findByUserIdAndCurrencyAndIsActiveTrue_shouldReturnEmptyWhenNotFound() {
        Optional<Wallet> found = walletRepository
                .findByUserIdAndCurrencyAndIsActiveTrue(UUID.randomUUID(), "USD");
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdAndCurrencyAndIsActiveTrue_shouldReturnEmptyForInactiveWallet() {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .availableBalance(new BigDecimal("500.00"))
                .reservedBalance(BigDecimal.ZERO)
                .build();
        Wallet saved = walletRepository.save(wallet);
        saved.setActive(false);
        walletRepository.save(saved);

        Optional<Wallet> found = walletRepository
                .findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD");
        assertThat(found).isEmpty();
    }

    @Test
    void findAllByUserIdAndIsActiveTrue_shouldReturnAllActiveWalletsForUser() {
        Wallet usdWallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .availableBalance(new BigDecimal("1000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        Wallet eurWallet = Wallet.builder()
                .userId(userId)
                .currency("EUR")
                .availableBalance(new BigDecimal("500.00"))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        walletRepository.save(usdWallet);
        walletRepository.save(eurWallet);

        List<Wallet> wallets = walletRepository.findAllByUserIdAndIsActiveTrue(userId);

        assertThat(wallets).hasSize(2);
        assertThat(wallets).extracting(Wallet::getCurrency)
                .containsExactlyInAnyOrder("USD", "EUR");
    }

    @Test
    void existsByUserIdAndCurrency_shouldReturnTrueWhenExists() {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .availableBalance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);

        boolean exists = walletRepository
                .existsByUserIdAndCurrency(userId, "USD");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndCurrency_shouldReturnFalseWhenNotExists() {
        boolean exists = walletRepository
                .existsByUserIdAndCurrency(UUID.randomUUID(), "USD");
        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldUpdateExistingWallet() {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .availableBalance(new BigDecimal("1000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .build();
        Wallet saved = walletRepository.save(wallet);

        saved.setAvailableBalance(new BigDecimal("1500.00"));
        Wallet updated = walletRepository.save(saved);

        assertThat(updated.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void delete_shouldRemoveWallet() {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .availableBalance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .build();
        Wallet saved = walletRepository.save(wallet);

        walletRepository.delete(saved);

        Optional<Wallet> found = walletRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}
