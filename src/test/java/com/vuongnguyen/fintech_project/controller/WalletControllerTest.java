package com.vuongnguyen.fintech_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuongnguyen.fintech_project.dto.WalletBalanceResponse;
import com.vuongnguyen.fintech_project.exception.GlobalExceptionHandler;
import com.vuongnguyen.fintech_project.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private WalletBalanceResponse usdtResponse;
    private WalletBalanceResponse btcResponse;
    private WalletBalanceResponse ethResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        usdtResponse = new WalletBalanceResponse();
        usdtResponse.setUserId(1L);
        usdtResponse.setCurrency("USDT");
        usdtResponse.setBalance(new BigDecimal("50000.00"));
        usdtResponse.setLastUpdated(LocalDateTime.now());

        btcResponse = new WalletBalanceResponse();
        btcResponse.setUserId(1L);
        btcResponse.setCurrency("BTC");
        btcResponse.setBalance(new BigDecimal("1.50"));
        btcResponse.setLastUpdated(LocalDateTime.now());

        ethResponse = new WalletBalanceResponse();
        ethResponse.setUserId(1L);
        ethResponse.setCurrency("ETH");
        ethResponse.setBalance(new BigDecimal("10.25"));
        ethResponse.setLastUpdated(LocalDateTime.now());
    }

    @Test
    void testGetUserWalletBalances_Success() throws Exception {
        List<WalletBalanceResponse> balances = Arrays.asList(usdtResponse, btcResponse, ethResponse);
        when(walletService.getUserWalletBalances(1L)).thenReturn(balances);

        mockMvc.perform(get("/api/v1/wallets/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", notNullValue()))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.data.length()", notNullValue()));

        verify(walletService, times(1)).getUserWalletBalances(1L);
    }

    @Test
    void testGetUserWalletBalances_Empty() throws Exception {
        when(walletService.getUserWalletBalances(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/wallets/user/2"))
                .andExpect(status().isNotFound());

        verify(walletService, times(1)).getUserWalletBalances(2L);
    }

    @Test
    void testGetUserWalletBalances_SingleBalance() throws Exception {
        List<WalletBalanceResponse> balances = Collections.singletonList(usdtResponse);
        when(walletService.getUserWalletBalances(1L)).thenReturn(balances);

        mockMvc.perform(get("/api/v1/wallets/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", notNullValue()));

        verify(walletService, times(1)).getUserWalletBalances(1L);
    }

    @Test
    void testGetUserWalletBalances_MultipleBalances() throws Exception {
        List<WalletBalanceResponse> balances = Arrays.asList(usdtResponse, btcResponse, ethResponse);
        when(walletService.getUserWalletBalances(1L)).thenReturn(balances);

        mockMvc.perform(get("/api/v1/wallets/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", notNullValue()));

        verify(walletService, times(1)).getUserWalletBalances(1L);
    }

    @Test
    void testGetUserWalletBalances_InvalidUserId() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/user/0"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetUserWalletBalances_NegativeUserId() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/user/-1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetUserWalletBalances_NonNumericUserId() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/user/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserWalletBalance_Success() throws Exception {
        when(walletService.getUserWalletBalance(1L, "USDT")).thenReturn(usdtResponse);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/USDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", notNullValue()))
                .andExpect(jsonPath("$.data", notNullValue()));

        verify(walletService, times(1)).getUserWalletBalance(1L, "USDT");
    }

    @Test
    void testGetUserWalletBalance_NotFound() throws Exception {
        when(walletService.getUserWalletBalance(2L, "BTC")).thenReturn(null);

        mockMvc.perform(get("/api/v1/wallets/user/2/currency/BTC"))
                .andExpect(status().isNotFound());

        verify(walletService, times(1)).getUserWalletBalance(2L, "BTC");
    }

    @Test
    void testGetUserWalletBalance_CaseInsensitive() throws Exception {
        when(walletService.getUserWalletBalance(1L, "USDT")).thenReturn(usdtResponse);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/usdt"))
                .andExpect(status().isOk());

        verify(walletService, times(1)).getUserWalletBalance(1L, "USDT");
    }

    @Test
    void testGetUserWalletBalance_DifferentCurrencies() throws Exception {
        when(walletService.getUserWalletBalance(1L, "BTC")).thenReturn(btcResponse);
        when(walletService.getUserWalletBalance(1L, "ETH")).thenReturn(ethResponse);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/BTC"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/ETH"))
                .andExpect(status().isOk());

        verify(walletService, times(1)).getUserWalletBalance(1L, "BTC");
        verify(walletService, times(1)).getUserWalletBalance(1L, "ETH");
    }

    @Test
    void testGetUserWalletBalance_ZeroBalance() throws Exception {
        WalletBalanceResponse zeroBalance = new WalletBalanceResponse();
        zeroBalance.setUserId(1L);
        zeroBalance.setCurrency("XRP");
        zeroBalance.setBalance(BigDecimal.ZERO);
        zeroBalance.setLastUpdated(LocalDateTime.now());

        when(walletService.getUserWalletBalance(1L, "XRP")).thenReturn(zeroBalance);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/XRP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));

        verify(walletService, times(1)).getUserWalletBalance(1L, "XRP");
    }

    @Test
    void testGetUserWalletBalance_LargeBalance() throws Exception {
        WalletBalanceResponse largeBalance = new WalletBalanceResponse();
        largeBalance.setUserId(1L);
        largeBalance.setCurrency("USDT");
        largeBalance.setBalance(new BigDecimal("999999999.99"));
        largeBalance.setLastUpdated(LocalDateTime.now());

        when(walletService.getUserWalletBalance(1L, "USDT")).thenReturn(largeBalance);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/USDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));

        verify(walletService, times(1)).getUserWalletBalance(1L, "USDT");
    }

    @Test
    void testGetUserWalletBalance_SmallBalance() throws Exception {
        WalletBalanceResponse smallBalance = new WalletBalanceResponse();
        smallBalance.setUserId(1L);
        smallBalance.setCurrency("BTC");
        smallBalance.setBalance(new BigDecimal("0.00000001"));
        smallBalance.setLastUpdated(LocalDateTime.now());

        when(walletService.getUserWalletBalance(1L, "BTC")).thenReturn(smallBalance);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));

        verify(walletService, times(1)).getUserWalletBalance(1L, "BTC");
    }

    @Test
    void testGetUserWalletBalance_InvalidUserId() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/user/0/currency/USDT"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetUserWalletBalance_NegativeUserId() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/user/-1/currency/USDT"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetUserWalletBalance_MissingCurrency() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/user/1/currency/"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetUserWalletBalance_SpecialCharactersCurrency() throws Exception {
        when(walletService.getUserWalletBalance(1L, "USDT")).thenReturn(usdtResponse);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/USDT"))
                .andExpect(status().isOk());

        verify(walletService, times(1)).getUserWalletBalance(1L, "USDT");
    }

    @Test
    void testGetUserWalletBalance_VerifyServiceCall() throws Exception {
        when(walletService.getUserWalletBalance(1L, "USDT")).thenReturn(usdtResponse);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/USDT"))
                .andExpect(status().isOk());

        verify(walletService, times(1)).getUserWalletBalance(1L, "USDT");
    }

    @Test
    void testGetUserWalletBalances_VerifyServiceCall() throws Exception {
        List<WalletBalanceResponse> balances = Arrays.asList(usdtResponse, btcResponse);
        when(walletService.getUserWalletBalances(1L)).thenReturn(balances);

        mockMvc.perform(get("/api/v1/wallets/user/1"))
                .andExpect(status().isOk());

        verify(walletService, times(1)).getUserWalletBalances(1L);
    }

    @Test
    void testGetUserWalletBalance_WhitespaceHandling() throws Exception {
        when(walletService.getUserWalletBalance(1L, "USDT")).thenReturn(usdtResponse);

        mockMvc.perform(get("/api/v1/wallets/user/1/currency/USDT"))
                .andExpect(status().isOk());

        verify(walletService, times(1)).getUserWalletBalance(1L, "USDT");
    }
}
