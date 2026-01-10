package com.vuongnguyen.fintech_project.dto;

import com.vuongnguyen.fintech_project.entity.WalletBalance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceResponse {

    private Long userId;
    private String currency;
    private BigDecimal balance;
    private LocalDateTime lastUpdated;

    public WalletBalanceResponse toWalletBalanceResponse(WalletBalance walletBalance) {
        this.userId = walletBalance.getUser().getId();
        this.currency = walletBalance.getCurrency();
        this.balance = walletBalance.getBalance() != null
                ? walletBalance.getBalance().stripTrailingZeros()
                : BigDecimal.ZERO;
        this.lastUpdated = walletBalance.getUpdatedAt();
        return this;
    }
}
