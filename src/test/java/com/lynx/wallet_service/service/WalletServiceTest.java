package com.lynx.wallet_service.service;

import com.lynx.wallet_service.wallet.dto.request.*;
import com.lynx.wallet_service.wallet.dto.response.*;
import com.lynx.wallet_service.wallet.entity.Wallet;
import com.lynx.wallet_service.wallet.entity.TransactionType;
import com.lynx.wallet_service.wallet.entity.WalletTransaction;
import com.lynx.wallet_service.wallet.exception.InsufficientFundsException;
import com.lynx.wallet_service.wallet.exception.InsufficientReservedBalanceException;
import com.lynx.wallet_service.wallet.exception.WalletNotFoundException;
import com.lynx.wallet_service.wallet.repository.WalletRepository;
import com.lynx.wallet_service.wallet.repository.WalletTransactionRepository;
import com.lynx.wallet_service.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID userId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .currency("USD")
                .availableBalance(new BigDecimal("1000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    // ─── Deposit Tests ────────────────────────────────────────────────────────

    @Test
    void deposit_shouldCreateNewWalletAndReturnDepositResponse() {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD"))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(WalletTransaction.builder()
                        .id(UUID.randomUUID())
                        .walletId(wallet.getId())
                        .transactionType(TransactionType.DEPOSIT)
                        .amount(request.getAmount())
                        .createdAt(LocalDateTime.now())
                        .build());

        DepositResponse response = walletService.deposit(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Deposit successful");
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void deposit_shouldAddToExistingWallet() {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(WalletTransaction.builder()
                        .id(UUID.randomUUID())
                        .walletId(wallet.getId())
                        .transactionType(TransactionType.DEPOSIT)
                        .amount(request.getAmount())
                        .createdAt(LocalDateTime.now())
                        .build());

        DepositResponse response = walletService.deposit(userId, request);

        assertThat(response).isNotNull();
        assertThat(wallet.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    // ─── Withdraw Tests ───────────────────────────────────────────────────────

    @Test
    void withdraw_shouldReduceAvailableBalance() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(new BigDecimal("200.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(WalletTransaction.builder()
                        .id(UUID.randomUUID())
                        .walletId(wallet.getId())
                        .transactionType(TransactionType.WITHDRAWAL)
                        .amount(request.getAmount())
                        .createdAt(LocalDateTime.now())
                        .build());

        WithdrawalResponse response = walletService.withdraw(userId, request);

        assertThat(response).isNotNull();
        assertThat(wallet.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    void withdraw_shouldThrowInsufficientFundsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(new BigDecimal("9999.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.withdraw(userId, request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void withdraw_shouldThrowWalletNotFoundException() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.withdraw(userId, request))
                .isInstanceOf(WalletNotFoundException.class);
    }

    // ─── Reserve Tests ────────────────────────────────────────────────────────

    @Test
    void reserveFunds_shouldMoveBalanceToReserved() {
        ReserveFundsRequest request = new ReserveFundsRequest();
        request.setUserId(userId);
        request.setAmount(new BigDecimal("300.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(WalletTransaction.builder().build());

        walletService.reserveFunds(request);

        assertThat(wallet.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(wallet.getReservedBalance())
                .isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void reserveFunds_shouldThrowInsufficientFundsException() {
        ReserveFundsRequest request = new ReserveFundsRequest();
        request.setUserId(userId);
        request.setAmount(new BigDecimal("9999.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.reserveFunds(request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    // ─── Release Tests ────────────────────────────────────────────────────────

    @Test
    void releaseFunds_shouldRestoreAvailableBalance() {
        wallet.setReservedBalance(new BigDecimal("300.00"));
        wallet.setAvailableBalance(new BigDecimal("700.00"));

        ReleaseFundsRequest request = new ReleaseFundsRequest();
        request.setUserId(userId);
        request.setAmount(new BigDecimal("300.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(WalletTransaction.builder().build());

        walletService.releaseFunds(request);

        assertThat(wallet.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(wallet.getReservedBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void releaseFunds_shouldThrowInsufficientReservedBalanceException() {
        ReleaseFundsRequest request = new ReleaseFundsRequest();
        request.setUserId(userId);
        request.setAmount(new BigDecimal("9999.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.releaseFunds(request))
                .isInstanceOf(InsufficientReservedBalanceException.class);
    }

    // ─── Capture Tests ────────────────────────────────────────────────────────

    @Test
    void captureFunds_shouldDeductReservedAndReturnRemainder() {
        wallet.setReservedBalance(new BigDecimal("500.00"));
        wallet.setAvailableBalance(new BigDecimal("500.00"));

        CaptureFundsRequest request = new CaptureFundsRequest();
        request.setUserId(userId);
        request.setReservedAmount(new BigDecimal("500.00"));
        request.setActualCost(new BigDecimal("480.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(WalletTransaction.builder().build());

        walletService.captureFunds(request);

        assertThat(wallet.getReservedBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("520.00"));
    }

    @Test
    void captureFunds_shouldThrowInsufficientReservedBalanceException() {
        CaptureFundsRequest request = new CaptureFundsRequest();
        request.setUserId(userId);
        request.setReservedAmount(new BigDecimal("9999.00"));
        request.setActualCost(new BigDecimal("9999.00"));
        request.setCurrency("USD");

        when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.captureFunds(request))
                .isInstanceOf(InsufficientReservedBalanceException.class);
    }

    // ─── Get Wallet Tests ─────────────────────────────────────────────────────

    @Test
    void getWallet_shouldReturnWalletResponse() {
        when(walletRepository.findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD"))
                .thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWallet(userId, "USD");

        assertThat(response).isNotNull();
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getAvailableBalance())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void getWallet_shouldThrowWalletNotFoundException() {
        when(walletRepository.findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(userId, "USD"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void getTransactionHistory_shouldReturnPaginatedTransactions() {
        WalletTransaction transaction = WalletTransaction.builder()
                .id(UUID.randomUUID())
                .walletId(wallet.getId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        Page<WalletTransaction> page = new PageImpl<>(List.of(transaction));

        when(walletRepository.findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD"))
                .thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findAllByWalletId(any(UUID.class), any(Pageable.class)))
                .thenReturn(page);

        TransactionHistoryResponse response = walletService.getTransactionHistory(userId, "USD", 1, 10);

        assertThat(response).isNotNull();
        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getTotalRecords()).isEqualTo(1);
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getLimit()).isEqualTo(10);
    }

    @Test
    void getTransactionHistory_shouldThrowWalletNotFoundException() {
        when(walletRepository.findByUserIdAndCurrencyAndIsActiveTrue(userId, "USD"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getTransactionHistory(userId, "USD", 1, 10))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
