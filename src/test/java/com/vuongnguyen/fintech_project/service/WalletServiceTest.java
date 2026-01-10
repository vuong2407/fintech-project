package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.WalletBalanceResponse;
import com.vuongnguyen.fintech_project.entity.User;
import com.vuongnguyen.fintech_project.entity.WalletBalance;
import com.vuongnguyen.fintech_project.repository.WalletBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletBalanceRepository walletBalanceRepository;

    @InjectMocks
    private WalletService walletService;

    private User testUser;
    private WalletBalance usdtBalance;
    private WalletBalance btcBalance;
    private WalletBalance ethBalance;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("vuongnguyen");
        testUser.setEmail("vanvuong24072001@gmail.com");
        testUser.setCreatedAt(LocalDateTime.now());

        usdtBalance = new WalletBalance();
        usdtBalance.setId(1L);
        usdtBalance.setUser(testUser);
        usdtBalance.setCurrency("USDT");
        usdtBalance.setBalance(new BigDecimal("50000.00"));
        usdtBalance.setVersion(1L);
        usdtBalance.setUpdatedAt(LocalDateTime.now());

        btcBalance = new WalletBalance();
        btcBalance.setId(2L);
        btcBalance.setUser(testUser);
        btcBalance.setCurrency("BTC");
        btcBalance.setBalance(new BigDecimal("1.50"));
        btcBalance.setVersion(1L);
        btcBalance.setUpdatedAt(LocalDateTime.now());

        ethBalance = new WalletBalance();
        ethBalance.setId(3L);
        ethBalance.setUser(testUser);
        ethBalance.setCurrency("ETH");
        ethBalance.setBalance(new BigDecimal("10.25"));
        ethBalance.setVersion(1L);
        ethBalance.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGetUserWalletBalances_Success() {
        when(walletBalanceRepository.findByUserId(1L))
                .thenReturn(Arrays.asList(usdtBalance, btcBalance, ethBalance));

        List<WalletBalanceResponse> responses = walletService.getUserWalletBalances(1L);

        assertNotNull(responses);
        assertEquals(3, responses.size());
        assertEquals("USDT", responses.get(0).getCurrency());
        assertEquals("BTC", responses.get(1).getCurrency());
        assertEquals("ETH", responses.get(2).getCurrency());
        assertEquals(0, new BigDecimal("50000.00").compareTo(responses.get(0).getBalance()));
        verify(walletBalanceRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetUserWalletBalances_EmptyList() {
        when(walletBalanceRepository.findByUserId(2L))
                .thenReturn(Collections.emptyList());

        List<WalletBalanceResponse> responses = walletService.getUserWalletBalances(2L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(walletBalanceRepository, times(1)).findByUserId(2L);
    }

    @Test
    void testGetUserWalletBalances_SingleBalance() {
        when(walletBalanceRepository.findByUserId(1L))
                .thenReturn(Collections.singletonList(usdtBalance));

        List<WalletBalanceResponse> responses = walletService.getUserWalletBalances(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("USDT", responses.get(0).getCurrency());
        assertEquals(1L, responses.get(0).getUserId());
        verify(walletBalanceRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetUserWalletBalances_VerifyUserIdMapping() {
        when(walletBalanceRepository.findByUserId(1L))
                .thenReturn(Arrays.asList(usdtBalance, btcBalance));

        List<WalletBalanceResponse> responses = walletService.getUserWalletBalances(1L);

        assertNotNull(responses);
        for (WalletBalanceResponse response : responses) {
            assertEquals(1L, response.getUserId());
        }
    }

    @Test
    void testGetUserWalletBalances_VerifyLastUpdatedMapping() {
        when(walletBalanceRepository.findByUserId(1L))
                .thenReturn(Collections.singletonList(usdtBalance));

        List<WalletBalanceResponse> responses = walletService.getUserWalletBalances(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertNotNull(responses.get(0).getLastUpdated());
        assertEquals(usdtBalance.getUpdatedAt(), responses.get(0).getLastUpdated());
    }

    @Test
    void testGetUserWalletBalance_Success() {
        when(walletBalanceRepository.findByUserIdAndCurrency(1L, "USDT"))
                .thenReturn(Optional.of(usdtBalance));

        WalletBalanceResponse response = walletService.getUserWalletBalance(1L, "USDT");

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("USDT", response.getCurrency());
        assertEquals(0, new BigDecimal("50000.00").compareTo(response.getBalance()));
        verify(walletBalanceRepository, times(1)).findByUserIdAndCurrency(1L, "USDT");
    }

    @Test
    void testGetUserWalletBalance_NotFound() {
        when(walletBalanceRepository.findByUserIdAndCurrency(2L, "BTC"))
                .thenReturn(Optional.empty());

        WalletBalanceResponse response = walletService.getUserWalletBalance(2L, "BTC");

        assertNull(response);
        verify(walletBalanceRepository, times(1)).findByUserIdAndCurrency(2L, "BTC");
    }

    @Test
    void testGetUserWalletBalance_CaseInsensitive() {
        when(walletBalanceRepository.findByUserIdAndCurrency(1L, "USDT"))
                .thenReturn(Optional.of(usdtBalance));

        WalletBalanceResponse response = walletService.getUserWalletBalance(1L, "usdt");

        assertNotNull(response);
        assertEquals("USDT", response.getCurrency());
        verify(walletBalanceRepository, times(1)).findByUserIdAndCurrency(1L, "USDT");
    }

    @Test
    void testGetUserWalletBalance_DifferentCurrencies() {
        when(walletBalanceRepository.findByUserIdAndCurrency(1L, "BTC"))
                .thenReturn(Optional.of(btcBalance));

        WalletBalanceResponse response = walletService.getUserWalletBalance(1L, "BTC");

        assertNotNull(response);
        assertEquals("BTC", response.getCurrency());
        assertEquals(0, new BigDecimal("1.50").compareTo(response.getBalance()));
    }

    @Test
    void testGetUserWalletBalance_ZeroBalance() {
        WalletBalance zeroBalance = new WalletBalance();
        zeroBalance.setId(4L);
        zeroBalance.setUser(testUser);
        zeroBalance.setCurrency("XRP");
        zeroBalance.setBalance(BigDecimal.ZERO);
        zeroBalance.setVersion(1L);
        zeroBalance.setUpdatedAt(LocalDateTime.now());

        when(walletBalanceRepository.findByUserIdAndCurrency(1L, "XRP"))
                .thenReturn(Optional.of(zeroBalance));

        WalletBalanceResponse response = walletService.getUserWalletBalance(1L, "XRP");

        assertNotNull(response);
        assertEquals("XRP", response.getCurrency());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getBalance()));
    }

    @Test
    void testGetUserWalletBalance_LargeBalance() {
        WalletBalance largeBalance = new WalletBalance();
        largeBalance.setId(5L);
        largeBalance.setUser(testUser);
        largeBalance.setCurrency("USDT");
        largeBalance.setBalance(new BigDecimal("999999999.99"));
        largeBalance.setVersion(1L);
        largeBalance.setUpdatedAt(LocalDateTime.now());

        when(walletBalanceRepository.findByUserIdAndCurrency(1L, "USDT"))
                .thenReturn(Optional.of(largeBalance));

        WalletBalanceResponse response = walletService.getUserWalletBalance(1L, "USDT");

        assertNotNull(response);
        assertEquals(0, new BigDecimal("999999999.99").compareTo(response.getBalance()));
    }

    @Test
    void testGetUserWalletBalance_SmallBalance() {
        WalletBalance smallBalance = new WalletBalance();
        smallBalance.setId(6L);
        smallBalance.setUser(testUser);
        smallBalance.setCurrency("BTC");
        smallBalance.setBalance(new BigDecimal("0.00000001"));
        smallBalance.setVersion(1L);
        smallBalance.setUpdatedAt(LocalDateTime.now());

        when(walletBalanceRepository.findByUserIdAndCurrency(1L, "BTC"))
                .thenReturn(Optional.of(smallBalance));

        WalletBalanceResponse response = walletService.getUserWalletBalance(1L, "BTC");

        assertNotNull(response);
        assertEquals(0, new BigDecimal("0.00000001").compareTo(response.getBalance()));
    }

    @Test
    void testGetUserWalletBalances_MultipleUsers() {
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        WalletBalance user2Balance = new WalletBalance();
        user2Balance.setId(7L);
        user2Balance.setUser(user2);
        user2Balance.setCurrency("USDT");
        user2Balance.setBalance(new BigDecimal("10000.00"));
        user2Balance.setVersion(1L);
        user2Balance.setUpdatedAt(LocalDateTime.now());

        when(walletBalanceRepository.findByUserId(1L))
                .thenReturn(Arrays.asList(usdtBalance, btcBalance));
        when(walletBalanceRepository.findByUserId(2L))
                .thenReturn(Collections.singletonList(user2Balance));

        List<WalletBalanceResponse> user1Balances = walletService.getUserWalletBalances(1L);
        List<WalletBalanceResponse> user2Balances = walletService.getUserWalletBalances(2L);

        assertEquals(2, user1Balances.size());
        assertEquals(1, user2Balances.size());
        assertEquals(1L, user1Balances.get(0).getUserId());
        assertEquals(2L, user2Balances.get(0).getUserId());
    }

    @Test
    void testGetUserWalletBalance_VerifyRepositoryCall() {
        when(walletBalanceRepository.findByUserIdAndCurrency(1L, "ETH"))
                .thenReturn(Optional.of(ethBalance));

        walletService.getUserWalletBalance(1L, "ETH");

        verify(walletBalanceRepository, times(1)).findByUserIdAndCurrency(1L, "ETH");
    }
}
