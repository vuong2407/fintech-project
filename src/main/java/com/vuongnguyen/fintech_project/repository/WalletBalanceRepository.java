package com.vuongnguyen.fintech_project.repository;

import com.vuongnguyen.fintech_project.entity.WalletBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, Long> {

    List<WalletBalance> findByUserId(Long userId);

    Optional<WalletBalance> findByUserIdAndCurrency(Long userId, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.user.id = :userId AND wb.currency = :currency")
    Optional<WalletBalance> findByUserIdAndCurrencyWithLock(@Param("userId") Long userId, @Param("currency") String currency);
}
