package com.vuongnguyen.fintech_project.config;

import com.vuongnguyen.fintech_project.entity.User;
import com.vuongnguyen.fintech_project.entity.WalletBalance;
import com.vuongnguyen.fintech_project.repository.UserRepository;
import com.vuongnguyen.fintech_project.repository.WalletBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletBalanceRepository walletBalanceRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUserAndWallet();
    }

    private void initializeDefaultUserAndWallet() {
        if (userRepository.findByUsername("vuongnguyen").isPresent()) {
            log.info("vuongnguyen already exists, skipping initialization");
            return;
        }

        User defaultUser = new User();
        defaultUser.setUsername("vuongnguyen");
        defaultUser.setEmail("vanvuong24072001@gmail.com");
        defaultUser.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(defaultUser);
        log.info("Created default user with ID: {}", savedUser.getId());

        createWalletBalance(savedUser, "USDT", new BigDecimal("50000.00000000"));
        createWalletBalance(savedUser, "ETH", BigDecimal.ZERO);
        createWalletBalance(savedUser, "BTC", BigDecimal.ZERO);

        log.info("Initialized wallet balances for user: {}", savedUser.getUsername());
    }

    private void createWalletBalance(User user, String currency, BigDecimal balance) {
        WalletBalance walletBalance = new WalletBalance();
        walletBalance.setUser(user);
        walletBalance.setCurrency(currency);
        walletBalance.setBalance(balance);
        walletBalance.setUpdatedAt(LocalDateTime.now());

        walletBalanceRepository.save(walletBalance);
        log.info("Created wallet balance: {} {} for user {}", balance, currency, user.getUsername());
    }
}
