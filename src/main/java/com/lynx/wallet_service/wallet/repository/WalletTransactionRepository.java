package com.lynx.wallet_service.wallet.repository;

import com.lynx.wallet_service.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Page<WalletTransaction> findAllByWalletId(UUID walletId, Pageable pageable);

    Page<WalletTransaction> findAllByWalletIdIn(List<UUID> walletIds, Pageable pageable);
}