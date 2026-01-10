package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.TradeRequest;
import com.vuongnguyen.fintech_project.dto.TradeResponse;
import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.entity.Trade;
import com.vuongnguyen.fintech_project.entity.User;
import com.vuongnguyen.fintech_project.entity.WalletBalance;
import com.vuongnguyen.fintech_project.enums.TradeSide;
import com.vuongnguyen.fintech_project.exception.InsufficientBalanceException;
import com.vuongnguyen.fintech_project.exception.PriceNotAvailableException;
import com.vuongnguyen.fintech_project.exception.ResourceNotFoundException;
import com.vuongnguyen.fintech_project.exception.TradingException;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import com.vuongnguyen.fintech_project.repository.TradeRepository;
import com.vuongnguyen.fintech_project.repository.UserRepository;
import com.vuongnguyen.fintech_project.repository.WalletBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletBalanceRepository walletBalanceRepository;

    @Mock
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradingService tradingService;

    private User testUser;
    private TradeRequest tradeRequest;
    private AggregatedPrice aggregatedPrice;
    private WalletBalance usdtBalance;
    private WalletBalance btcBalance;
    private Trade savedTrade;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("vuongnguyen");
        testUser.setEmail("vanvuong24072001@gmail.com");
        testUser.setCreatedAt(LocalDateTime.now());

        tradeRequest = new TradeRequest();
        tradeRequest.setUserId(1L);
        tradeRequest.setSymbol("BTCUSDT");
        tradeRequest.setSide(TradeSide.BUY);
        tradeRequest.setQuantity(new BigDecimal("0.5"));
        tradeRequest.setClientOrderId("order-123");

        aggregatedPrice = new AggregatedPrice();
        aggregatedPrice.setId(1L);
        aggregatedPrice.setSymbol("BTCUSDT");
        aggregatedPrice.setBestBid(new BigDecimal("50000.00"));
        aggregatedPrice.setBestAsk(new BigDecimal("50001.00"));
        aggregatedPrice.setTimestamp(LocalDateTime.now());

        usdtBalance = new WalletBalance();
        usdtBalance.setId(1L);
        usdtBalance.setUser(testUser);
        usdtBalance.setCurrency("USDT");
        usdtBalance.setBalance(new BigDecimal("100000.00"));
        usdtBalance.setVersion(1L);
        usdtBalance.setUpdatedAt(LocalDateTime.now());

        btcBalance = new WalletBalance();
        btcBalance.setId(2L);
        btcBalance.setUser(testUser);
        btcBalance.setCurrency("BTC");
        btcBalance.setBalance(new BigDecimal("0.00"));
        btcBalance.setVersion(1L);
        btcBalance.setUpdatedAt(LocalDateTime.now());

        savedTrade = new Trade();
        savedTrade.setId(1L);
        savedTrade.setUser(testUser);
        savedTrade.setSymbol("BTCUSDT");
        savedTrade.setSide(TradeSide.BUY);
        savedTrade.setPrice(new BigDecimal("50001.00"));
        savedTrade.setQuantity(new BigDecimal("0.5"));
        savedTrade.setTotalAmount(new BigDecimal("25000.50"));
        savedTrade.setClientOrderId("order-123");
        savedTrade.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testExecuteUserTrading_SuccessfulBuyOrder() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        TradeResponse response = tradingService.executeUserTrading(tradeRequest);

        assertNotNull(response);
        assertEquals(1L, response.getTradeId());
        assertEquals(1L, response.getUserId());
        assertEquals("BTCUSDT", response.getSymbol());
        assertEquals(TradeSide.BUY, response.getSide());
        assertEquals(new BigDecimal("0.5"), response.getQuantity());

        verify(tradeRepository, times(1)).findByClientOrderId("order-123");
        verify(userRepository, times(1)).findById(1L);
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("BTCUSDT");
        verify(walletBalanceRepository, times(2)).findByUserIdAndCurrencyWithLock(anyLong(), anyString());
        verify(walletBalanceRepository, times(1)).saveAll(anyList());
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_SuccessfulSellOrder() {
        tradeRequest.setSide(TradeSide.SELL);
        aggregatedPrice.setBestBid(new BigDecimal("50000.00"));
        btcBalance.setBalance(new BigDecimal("1.0"));

        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));

        savedTrade.setSide(TradeSide.SELL);
        savedTrade.setPrice(new BigDecimal("50000.00"));
        savedTrade.setTotalAmount(new BigDecimal("25000.00"));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        TradeResponse response = tradingService.executeUserTrading(tradeRequest);

        assertNotNull(response);
        assertEquals(TradeSide.SELL, response.getSide());
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_Idempotency_DuplicateClientOrderId() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.of(savedTrade));

        TradeResponse response = tradingService.executeUserTrading(tradeRequest);

        assertNotNull(response);
        assertEquals(1L, response.getTradeId());
        assertEquals("order-123", response.getClientOrderId());

        verify(tradeRepository, times(1)).findByClientOrderId("order-123");
        verify(userRepository, never()).findById(anyLong());
        verify(aggregatedPriceRepository, never()).findLatestBySymbol(anyString());
        verify(walletBalanceRepository, never()).findByUserIdAndCurrencyWithLock(anyLong(), anyString());
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_UserNotFound() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tradingService.executeUserTrading(tradeRequest));

        verify(userRepository, times(1)).findById(1L);
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_PriceNotAvailable() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.empty());

        assertThrows(PriceNotAvailableException.class, () -> tradingService.executeUserTrading(tradeRequest));

        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("BTCUSDT");
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_InsufficientUSDTBalance() {
        usdtBalance.setBalance(new BigDecimal("1000.00"));

        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));

        assertThrows(InsufficientBalanceException.class, () -> tradingService.executeUserTrading(tradeRequest));

        verify(walletBalanceRepository, never()).saveAll(anyList());
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_InsufficientAssetBalance_Sell() {
        tradeRequest.setSide(TradeSide.SELL);
        btcBalance.setBalance(new BigDecimal("0.1"));

        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));

        assertThrows(InsufficientBalanceException.class, () -> tradingService.executeUserTrading(tradeRequest));

        verify(walletBalanceRepository, never()).saveAll(anyList());
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_USDTWalletNotFound() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.empty());

        assertThrows(TradingException.class, () -> tradingService.executeUserTrading(tradeRequest));

        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_AssetWalletNotFound() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.empty());

        assertThrows(TradingException.class, () -> tradingService.executeUserTrading(tradeRequest));

        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_RaceCondition_OptimisticLockingFailure() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));
        when(walletBalanceRepository.saveAll(anyList())).thenThrow(new OptimisticLockingFailureException("Concurrent modification detected"));

        assertThrows(OptimisticLockingFailureException.class, () -> tradingService.executeUserTrading(tradeRequest));

        verify(walletBalanceRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testExecuteUserTrading_BalanceUpdatedCorrectly_Buy() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        BigDecimal initialUSDT = usdtBalance.getBalance();
        BigDecimal initialBTC = btcBalance.getBalance();

        tradingService.executeUserTrading(tradeRequest);

        ArgumentCaptor<List<WalletBalance>> balancesCaptor = ArgumentCaptor.forClass(List.class);
        verify(walletBalanceRepository).saveAll(balancesCaptor.capture());

        List<WalletBalance> savedBalances = balancesCaptor.getValue();
        assertEquals(2, savedBalances.size());

        WalletBalance updatedUSDT = savedBalances.stream()
                .filter(wb -> "USDT".equals(wb.getCurrency()))
                .findFirst()
                .orElse(null);
        WalletBalance updatedBTC = savedBalances.stream()
                .filter(wb -> "BTC".equals(wb.getCurrency()))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedUSDT);
        assertNotNull(updatedBTC);

        BigDecimal expectedUSDT = initialUSDT.subtract(new BigDecimal("25000.50"));
        BigDecimal expectedBTC = initialBTC.add(new BigDecimal("0.5"));

        assertEquals(0, expectedUSDT.compareTo(updatedUSDT.getBalance()));
        assertEquals(0, expectedBTC.compareTo(updatedBTC.getBalance()));
    }

    @Test
    void testExecuteUserTrading_BalanceUpdatedCorrectly_Sell() {
        tradeRequest.setSide(TradeSide.SELL);
        btcBalance.setBalance(new BigDecimal("1.0"));

        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));

        savedTrade.setSide(TradeSide.SELL);
        savedTrade.setPrice(new BigDecimal("50000.00"));
        savedTrade.setTotalAmount(new BigDecimal("25000.00"));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        BigDecimal initialUSDT = usdtBalance.getBalance();
        BigDecimal initialBTC = btcBalance.getBalance();

        tradingService.executeUserTrading(tradeRequest);

        ArgumentCaptor<List<WalletBalance>> balancesCaptor = ArgumentCaptor.forClass(List.class);
        verify(walletBalanceRepository).saveAll(balancesCaptor.capture());

        List<WalletBalance> savedBalances = balancesCaptor.getValue();

        WalletBalance updatedUSDT = savedBalances.stream()
                .filter(wb -> "USDT".equals(wb.getCurrency()))
                .findFirst()
                .orElse(null);
        WalletBalance updatedBTC = savedBalances.stream()
                .filter(wb -> "BTC".equals(wb.getCurrency()))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedUSDT);
        assertNotNull(updatedBTC);

        BigDecimal expectedUSDT = initialUSDT.add(new BigDecimal("25000.00"));
        BigDecimal expectedBTC = initialBTC.subtract(new BigDecimal("0.5"));

        assertEquals(0, expectedUSDT.compareTo(updatedUSDT.getBalance()));
        assertEquals(0, expectedBTC.compareTo(updatedBTC.getBalance()));
    }

    @Test
    void testExecuteUserTrading_WithoutClientOrderId() {
        tradeRequest.setClientOrderId(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        TradeResponse response = tradingService.executeUserTrading(tradeRequest);

        assertNotNull(response);
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_LargeQuantity() {
        tradeRequest.setQuantity(new BigDecimal("10.5"));
        usdtBalance.setBalance(new BigDecimal("1000000.00"));

        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));

        savedTrade.setQuantity(new BigDecimal("10.5"));
        savedTrade.setTotalAmount(new BigDecimal("525010.50"));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        TradeResponse response = tradingService.executeUserTrading(tradeRequest);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("10.5").compareTo(response.getQuantity()));
    }

    @Test
    void testExecuteUserTrading_DifferentSymbols() {
        tradeRequest.setSymbol("ETHUSDT");

        AggregatedPrice ethPrice = new AggregatedPrice();
        ethPrice.setId(2L);
        ethPrice.setSymbol("ETHUSDT");
        ethPrice.setBestBid(new BigDecimal("3000.00"));
        ethPrice.setBestAsk(new BigDecimal("3001.00"));
        ethPrice.setTimestamp(LocalDateTime.now());

        WalletBalance ethBalance = new WalletBalance();
        ethBalance.setId(3L);
        ethBalance.setUser(testUser);
        ethBalance.setCurrency("ETH");
        ethBalance.setBalance(new BigDecimal("0.00"));
        ethBalance.setVersion(1L);
        ethBalance.setUpdatedAt(LocalDateTime.now());

        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("ETHUSDT")).thenReturn(Optional.of(ethPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "ETH")).thenReturn(Optional.of(ethBalance));

        Trade ethTrade = new Trade();
        ethTrade.setId(2L);
        ethTrade.setUser(testUser);
        ethTrade.setSymbol("ETHUSDT");
        ethTrade.setSide(TradeSide.BUY);
        ethTrade.setPrice(new BigDecimal("3001.00"));
        ethTrade.setQuantity(new BigDecimal("0.5"));
        ethTrade.setTotalAmount(new BigDecimal("1500.50"));
        ethTrade.setClientOrderId("order-123");
        ethTrade.setCreatedAt(LocalDateTime.now());

        when(tradeRepository.save(any(Trade.class))).thenReturn(ethTrade);

        TradeResponse response = tradingService.executeUserTrading(tradeRequest);

        assertNotNull(response);
        assertEquals("ETHUSDT", response.getSymbol());
    }

    @Test
    void testExecuteUserTrading_ConcurrentTradesIdempotency() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.of(savedTrade));

        TradeResponse response1 = tradingService.executeUserTrading(tradeRequest);
        TradeResponse response2 = tradingService.executeUserTrading(tradeRequest);

        assertEquals(response1.getTradeId(), response2.getTradeId());
        assertEquals(response1.getClientOrderId(), response2.getClientOrderId());

        verify(tradeRepository, times(2)).findByClientOrderId("order-123");
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testExecuteUserTrading_VerifyTradeDetailsCalculation() {
        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        tradingService.executeUserTrading(tradeRequest);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());

        Trade capturedTrade = tradeCaptor.getValue();
        assertEquals(0, new BigDecimal("50001.00").compareTo(capturedTrade.getPrice()));
        assertEquals(0, new BigDecimal("0.5").compareTo(capturedTrade.getQuantity()));
        assertEquals(0, new BigDecimal("25000.50").compareTo(capturedTrade.getTotalAmount()));
    }

    @Test
    void testExecuteUserTrading_VerifyTradeDetailsCalculation_Sell() {
        tradeRequest.setSide(TradeSide.SELL);
        btcBalance.setBalance(new BigDecimal("1.0"));

        when(tradeRepository.findByClientOrderId("order-123")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT")).thenReturn(Optional.of(aggregatedPrice));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "USDT")).thenReturn(Optional.of(usdtBalance));
        when(walletBalanceRepository.findByUserIdAndCurrencyWithLock(1L, "BTC")).thenReturn(Optional.of(btcBalance));

        savedTrade.setSide(TradeSide.SELL);
        savedTrade.setPrice(new BigDecimal("50000.00"));
        savedTrade.setTotalAmount(new BigDecimal("25000.00"));
        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        tradingService.executeUserTrading(tradeRequest);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());

        Trade capturedTrade = tradeCaptor.getValue();
        assertEquals(new BigDecimal("50000.00"), capturedTrade.getPrice());
        assertEquals(TradeSide.SELL, capturedTrade.getSide());
    }
}
