package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.WalletBalanceResponse;
import com.vuongnguyen.fintech_project.entity.WalletBalance;
import com.vuongnguyen.fintech_project.repository.WalletBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletBalanceRepository walletBalanceRepository;

    public List<WalletBalanceResponse> getUserWalletBalances(Long userId) {
        log.debug("Fetching wallet balances for user: {}", userId);

        List<WalletBalance> walletBalances = walletBalanceRepository.findByUserId(userId);

        return walletBalances.stream()
                .map(walletBalance -> new WalletBalanceResponse().toWalletBalanceResponse(walletBalance))
                .collect(Collectors.toList());
    }

    public WalletBalanceResponse getUserWalletBalance(Long userId, String currency) {
        log.debug("Fetching wallet balance for user: {} and currency: {}", userId, currency);

        WalletBalance walletBalance = walletBalanceRepository
                .findByUserIdAndCurrency(userId, currency.toUpperCase())
                .orElse(null);

        if (Objects.isNull(walletBalance)) {
            log.warn("Wallet balance not found for user: {} and currency: {}", userId, currency);
            return null;
        }

        return new WalletBalanceResponse().toWalletBalanceResponse(walletBalance);
    }
}
