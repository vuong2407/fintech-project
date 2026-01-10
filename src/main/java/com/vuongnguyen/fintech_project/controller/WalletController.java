package com.vuongnguyen.fintech_project.controller;

import com.vuongnguyen.fintech_project.dto.ApiResponse;
import com.vuongnguyen.fintech_project.dto.WalletBalanceResponse;
import com.vuongnguyen.fintech_project.service.WalletService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<WalletBalanceResponse>>> getUserWalletBalances(
            @PathVariable @NotNull @Positive Long userId) {

        log.info("Received request for wallet balances of user: {}", userId);

        List<WalletBalanceResponse> walletBalances = walletService.getUserWalletBalances(userId);

        if (walletBalances.isEmpty()) {
            log.warn("No wallet balances found for user: {}", userId);
            return ResponseEntity.notFound().build();
        }

        log.info("Successfully retrieved {} wallet balances for user: {}", walletBalances.size(), userId);
        return ResponseEntity.ok(ApiResponse.success("Wallet balances retrieved successfully", walletBalances));
    }

    @GetMapping("/user/{userId}/currency/{currency}")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getUserWalletBalance(
            @PathVariable @NotNull @Positive Long userId,
            @PathVariable @NotNull String currency) {

        log.info("Received request for wallet balance of user: {} and currency: {}", userId, currency);

        String normalizedCurrency = currency.toUpperCase().trim();

        WalletBalanceResponse walletBalance = walletService.getUserWalletBalance(userId, normalizedCurrency);

        if (walletBalance == null) {
            log.warn("Wallet balance not found for user: {} and currency: {}", userId, normalizedCurrency);
            return ResponseEntity.notFound().build();
        }

        log.info("Successfully retrieved wallet balance for user: {} and currency: {}", userId, normalizedCurrency);
        return ResponseEntity.ok(ApiResponse.success("Wallet balance retrieved successfully", walletBalance));
    }
}
