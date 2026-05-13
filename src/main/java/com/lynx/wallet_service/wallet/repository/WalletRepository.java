package com.lynx.wallet_service.wallet.repository;

import com.lynx.wallet_service.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserIdAndCurrencyAndIsActiveTrue(UUID userId, String currency);

    List<Wallet> findAllByUserIdAndIsActiveTrue(UUID userId);

    boolean existsByUserIdAndCurrency(UUID userId, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.currency = :currency AND w.isActive = true")
    Optional<Wallet> findByUserIdAndCurrencyForUpdate(@Param("userId") UUID userId, @Param("currency") String currency);
}