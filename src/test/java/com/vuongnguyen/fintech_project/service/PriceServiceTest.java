package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.AggregatedPriceResponse;
import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private AggregatedPriceRepository aggregatedPriceRepository;

    @InjectMocks
    private PriceService priceService;

    private AggregatedPrice mockPrice;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.now();
        mockPrice = new AggregatedPrice();
        mockPrice.setId(1L);
        mockPrice.setSymbol("BTCUSDT");
        mockPrice.setBestBid(new BigDecimal("50000.00"));
        mockPrice.setBestAsk(new BigDecimal("50001.00"));
        mockPrice.setTimestamp(testTimestamp);
    }

    @Test
    void testGetLatestPrice_Success() {
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(mockPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("BTCUSDT");

        assertTrue(result.isPresent());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("BTCUSDT");
    }

    @Test
    void testGetLatestPrice_VerifyDataMapping() {
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(mockPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("BTCUSDT");

        assertTrue(result.isPresent());
        AggregatedPriceResponse response = result.get();
        assertEquals("BTCUSDT", response.getSymbol());
        assertEquals(new BigDecimal("50000.00"), response.getBestBid());
        assertEquals(new BigDecimal("50001.00"), response.getBestAsk());
        assertEquals(testTimestamp, response.getTimestamp());
    }

    @Test
    void testGetLatestPrice_NotFound() {
        when(aggregatedPriceRepository.findLatestBySymbol("UNKNOWN"))
                .thenReturn(Optional.empty());

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("UNKNOWN");

        assertFalse(result.isPresent());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("UNKNOWN");
    }

    @Test
    void testGetLatestPrice_ETHSymbol() {
        AggregatedPrice ethPrice = new AggregatedPrice();
        ethPrice.setId(2L);
        ethPrice.setSymbol("ETHUSDT");
        ethPrice.setBestBid(new BigDecimal("3000.00"));
        ethPrice.setBestAsk(new BigDecimal("3001.00"));
        ethPrice.setTimestamp(testTimestamp);

        when(aggregatedPriceRepository.findLatestBySymbol("ETHUSDT"))
                .thenReturn(Optional.of(ethPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("ETHUSDT");

        assertTrue(result.isPresent());
        assertEquals("ETHUSDT", result.get().getSymbol());
        assertEquals(new BigDecimal("3000.00"), result.get().getBestBid());
        assertEquals(new BigDecimal("3001.00"), result.get().getBestAsk());
    }

    @Test
    void testGetLatestPrice_VerifyBidAskValues() {
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(mockPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("BTCUSDT");

        assertTrue(result.isPresent());
        AggregatedPriceResponse response = result.get();
        assertNotNull(response.getBestBid());
        assertNotNull(response.getBestAsk());
        assertTrue(response.getBestBid().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getBestAsk().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testGetLatestPrice_VerifyTimestamp() {
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(mockPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("BTCUSDT");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getTimestamp());
        assertEquals(testTimestamp, result.get().getTimestamp());
    }

    @Test
    void testGetLatestPrice_LargePriceValues() {
        AggregatedPrice largePrice = new AggregatedPrice();
        largePrice.setId(3L);
        largePrice.setSymbol("BTCUSDT");
        largePrice.setBestBid(new BigDecimal("99999.99"));
        largePrice.setBestAsk(new BigDecimal("100000.01"));
        largePrice.setTimestamp(testTimestamp);

        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(largePrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("BTCUSDT");

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("99999.99"), result.get().getBestBid());
        assertEquals(new BigDecimal("100000.01"), result.get().getBestAsk());
    }

    @Test
    void testGetLatestPrice_SmallPriceValues() {
        AggregatedPrice smallPrice = new AggregatedPrice();
        smallPrice.setId(4L);
        smallPrice.setSymbol("BTCUSDT");
        smallPrice.setBestBid(new BigDecimal("0.01"));
        smallPrice.setBestAsk(new BigDecimal("0.02"));
        smallPrice.setTimestamp(testTimestamp);

        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(smallPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("BTCUSDT");

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("0.01"), result.get().getBestBid());
        assertEquals(new BigDecimal("0.02"), result.get().getBestAsk());
    }

    @Test
    void testGetLatestPrice_MultipleSymbolCalls() {
        AggregatedPrice btcPrice = new AggregatedPrice();
        btcPrice.setSymbol("BTCUSDT");
        btcPrice.setBestBid(new BigDecimal("50000.00"));
        btcPrice.setBestAsk(new BigDecimal("50001.00"));
        btcPrice.setTimestamp(testTimestamp);

        AggregatedPrice ethPrice = new AggregatedPrice();
        ethPrice.setSymbol("ETHUSDT");
        ethPrice.setBestBid(new BigDecimal("3000.00"));
        ethPrice.setBestAsk(new BigDecimal("3001.00"));
        ethPrice.setTimestamp(testTimestamp);

        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(btcPrice));
        when(aggregatedPriceRepository.findLatestBySymbol("ETHUSDT"))
                .thenReturn(Optional.of(ethPrice));

        Optional<AggregatedPriceResponse> btcResult = priceService.getLatestPrice("BTCUSDT");
        Optional<AggregatedPriceResponse> ethResult = priceService.getLatestPrice("ETHUSDT");

        assertTrue(btcResult.isPresent());
        assertTrue(ethResult.isPresent());
        assertEquals("BTCUSDT", btcResult.get().getSymbol());
        assertEquals("ETHUSDT", ethResult.get().getSymbol());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("BTCUSDT");
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("ETHUSDT");
    }

    @Test
    void testGetLatestPrice_CaseInsensitiveSymbol() {
        when(aggregatedPriceRepository.findLatestBySymbol("btcusdt"))
                .thenReturn(Optional.of(mockPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("btcusdt");

        assertTrue(result.isPresent());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("btcusdt");
    }

    @Test
    void testGetLatestPrice_ResponseIsNotNull() {
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(mockPrice));

        Optional<AggregatedPriceResponse> result = priceService.getLatestPrice("BTCUSDT");

        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertNotNull(result.get().getSymbol());
        assertNotNull(result.get().getBestBid());
        assertNotNull(result.get().getBestAsk());
        assertNotNull(result.get().getTimestamp());
    }

    @Test
    void testGetLatestPrice_RepositoryCalledOnce() {
        when(aggregatedPriceRepository.findLatestBySymbol("BTCUSDT"))
                .thenReturn(Optional.of(mockPrice));

        priceService.getLatestPrice("BTCUSDT");

        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("BTCUSDT");
    }
}
