package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.PriceData;
import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.enums.DCESource;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceAggregationServiceTest {

    @Mock
    private BinancePriceService binancePriceService;

    @Mock
    private HuobiPriceService huobiPriceService;

    @Mock
    private AggregatedPriceRepository aggregatedPriceRepository;

    @InjectMocks
    private PriceAggregationService priceAggregationService;

    private List<PriceData> binancePrices;
    private List<PriceData> huobiPrices;

    @BeforeEach
    void setUp() {
        binancePrices = Arrays.asList(
                new PriceData("ETHUSDT", new BigDecimal("3000.00"), new BigDecimal("3001.00"), DCESource.BINANCE),
                new PriceData("BTCUSDT", new BigDecimal("50000.00"), new BigDecimal("50001.00"), DCESource.BINANCE)
        );

        huobiPrices = Arrays.asList(
                new PriceData("ETHUSDT", new BigDecimal("2999.00"), new BigDecimal("3000.50"), DCESource.HUOBI),
                new PriceData("BTCUSDT", new BigDecimal("49999.00"), new BigDecimal("50000.50"), DCESource.HUOBI)
        );
    }

    @Test
    void testAggregateAndStorePrices_Success() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(huobiPrices);

        priceAggregationService.aggregateAndStorePrices();

        verify(binancePriceService, times(1)).fetchPrices();
        verify(huobiPriceService, times(1)).fetchPrices();
        verify(aggregatedPriceRepository, times(2)).save(any(AggregatedPrice.class));
    }

    @Test
    void testAggregateAndStorePrices_CalculatesBestBid() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(huobiPrices);

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(captor.capture());

        List<AggregatedPrice> savedPrices = captor.getAllValues();
        AggregatedPrice ethPrice = savedPrices.stream()
                .filter(p -> p.getSymbol().equals("ETHUSDT"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("3000.00"), ethPrice.getBestBid());
    }

    @Test
    void testAggregateAndStorePrices_CalculatesBestAsk() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(huobiPrices);

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(captor.capture());

        List<AggregatedPrice> savedPrices = captor.getAllValues();
        AggregatedPrice ethPrice = savedPrices.stream()
                .filter(p -> p.getSymbol().equals("ETHUSDT"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("3000.50"), ethPrice.getBestAsk());
    }

    @Test
    void testAggregateAndStorePrices_BothSymbols() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(huobiPrices);

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(captor.capture());

        List<AggregatedPrice> savedPrices = captor.getAllValues();
        
        AggregatedPrice btcPrice = savedPrices.stream()
                .filter(p -> p.getSymbol().equals("BTCUSDT"))
                .findFirst()
                .orElseThrow();

        assertEquals("BTCUSDT", btcPrice.getSymbol());
        assertEquals(new BigDecimal("50000.00"), btcPrice.getBestBid());
        assertEquals(new BigDecimal("50000.50"), btcPrice.getBestAsk());
    }

    @Test
    void testAggregateAndStorePrices_EmptyBinancePrices() {
        when(binancePriceService.fetchPrices()).thenReturn(Collections.emptyList());
        when(huobiPriceService.fetchPrices()).thenReturn(huobiPrices);

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(captor.capture());

        List<AggregatedPrice> savedPrices = captor.getAllValues();
        assertEquals(2, savedPrices.size());
    }

    @Test
    void testAggregateAndStorePrices_EmptyHuobiPrices() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(Collections.emptyList());

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(captor.capture());

        List<AggregatedPrice> savedPrices = captor.getAllValues();
        assertEquals(2, savedPrices.size());
    }

    @Test
    void testAggregateAndStorePrices_BothExchangesEmpty() {
        when(binancePriceService.fetchPrices()).thenReturn(Collections.emptyList());
        when(huobiPriceService.fetchPrices()).thenReturn(Collections.emptyList());

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, never()).save(any(AggregatedPrice.class));
    }

    @Test
    void testAggregateAndStorePrices_OnlyOneSymbolAvailable() {
        List<PriceData> singleSymbolBinance = Collections.singletonList(
                new PriceData("ETHUSDT", new BigDecimal("3000.00"), new BigDecimal("3001.00"), DCESource.BINANCE)
        );

        List<PriceData> singleSymbolHuobi = Collections.singletonList(
                new PriceData("ETHUSDT", new BigDecimal("2999.00"), new BigDecimal("3000.50"), DCESource.HUOBI)
        );

        when(binancePriceService.fetchPrices()).thenReturn(singleSymbolBinance);
        when(huobiPriceService.fetchPrices()).thenReturn(singleSymbolHuobi);

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(1)).save(captor.capture());

        AggregatedPrice savedPrice = captor.getValue();
        assertEquals("ETHUSDT", savedPrice.getSymbol());
    }

    @Test
    void testAggregateAndStorePrices_SingleExchangeData() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(Collections.emptyList());

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(captor.capture());

        List<AggregatedPrice> savedPrices = captor.getAllValues();
        AggregatedPrice ethPrice = savedPrices.stream()
                .filter(p -> p.getSymbol().equals("ETHUSDT"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("3000.00"), ethPrice.getBestBid());
        assertEquals(new BigDecimal("3001.00"), ethPrice.getBestAsk());
    }

    @Test
    void testAggregateAndStorePrices_VerifyTimestampSet() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(huobiPrices);

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(captor.capture());

        List<AggregatedPrice> savedPrices = captor.getAllValues();
        for (AggregatedPrice price : savedPrices) {
            assertNotNull(price.getTimestamp());
        }
    }

    @Test
    void testAggregateAndStorePrices_BestBidIsHighest() {
        List<PriceData> mixedPrices = Arrays.asList(
                new PriceData("ETHUSDT", new BigDecimal("3000.00"), new BigDecimal("3001.00"), DCESource.BINANCE),
                new PriceData("ETHUSDT", new BigDecimal("3005.00"), new BigDecimal("3006.00"), DCESource.HUOBI)
        );

        when(binancePriceService.fetchPrices()).thenReturn(mixedPrices.subList(0, 1));
        when(huobiPriceService.fetchPrices()).thenReturn(mixedPrices.subList(1, 2));

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(1)).save(captor.capture());

        AggregatedPrice savedPrice = captor.getValue();
        assertEquals(new BigDecimal("3005.00"), savedPrice.getBestBid());
    }

    @Test
    void testAggregateAndStorePrices_BestAskIsLowest() {
        List<PriceData> mixedPrices = Arrays.asList(
                new PriceData("ETHUSDT", new BigDecimal("3000.00"), new BigDecimal("3001.00"), DCESource.BINANCE),
                new PriceData("ETHUSDT", new BigDecimal("3005.00"), new BigDecimal("3000.50"), DCESource.HUOBI)
        );

        when(binancePriceService.fetchPrices()).thenReturn(mixedPrices.subList(0, 1));
        when(huobiPriceService.fetchPrices()).thenReturn(mixedPrices.subList(1, 2));

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(1)).save(captor.capture());

        AggregatedPrice savedPrice = captor.getValue();
        assertEquals(new BigDecimal("3000.50"), savedPrice.getBestAsk());
    }

    @Test
    void testGetLatestPrice_Success() {
        AggregatedPrice mockPrice = new AggregatedPrice();
        mockPrice.setSymbol("ETHUSDT");
        mockPrice.setBestBid(new BigDecimal("3000.00"));
        mockPrice.setBestAsk(new BigDecimal("3001.00"));

        when(aggregatedPriceRepository.findLatestBySymbol("ETHUSDT"))
                .thenReturn(Optional.of(mockPrice));

        Optional<AggregatedPrice> result = priceAggregationService.getLatestPrice("ETHUSDT");

        assertTrue(result.isPresent());
        assertEquals("ETHUSDT", result.get().getSymbol());
        assertEquals(new BigDecimal("3000.00"), result.get().getBestBid());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("ETHUSDT");
    }

    @Test
    void testGetLatestPrice_NotFound() {
        when(aggregatedPriceRepository.findLatestBySymbol("UNKNOWN"))
                .thenReturn(Optional.empty());

        Optional<AggregatedPrice> result = priceAggregationService.getLatestPrice("UNKNOWN");

        assertTrue(result.isEmpty());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbol("UNKNOWN");
    }

    @Test
    void testAggregateAndStorePrices_HandlesExceptionGracefully() {
        when(binancePriceService.fetchPrices()).thenReturn(binancePrices);
        when(huobiPriceService.fetchPrices()).thenReturn(huobiPrices);
        when(aggregatedPriceRepository.save(any(AggregatedPrice.class)))
                .thenThrow(new RuntimeException("Database error"))
                .thenReturn(null);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(2)).save(any(AggregatedPrice.class));
    }

    @Test
    void testAggregateAndStorePrices_MultipleExchangesSameSymbol() {
        List<PriceData> allPrices = Arrays.asList(
                new PriceData("BTCUSDT", new BigDecimal("50000.00"), new BigDecimal("50001.00"), DCESource.BINANCE),
                new PriceData("BTCUSDT", new BigDecimal("49999.00"), new BigDecimal("50000.50"), DCESource.HUOBI)
        );

        when(binancePriceService.fetchPrices()).thenReturn(allPrices.subList(0, 1));
        when(huobiPriceService.fetchPrices()).thenReturn(allPrices.subList(1, 2));

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);

        priceAggregationService.aggregateAndStorePrices();

        verify(aggregatedPriceRepository, times(1)).save(captor.capture());

        AggregatedPrice savedPrice = captor.getValue();
        assertEquals("BTCUSDT", savedPrice.getSymbol());
        assertEquals(new BigDecimal("50000.00"), savedPrice.getBestBid());
        assertEquals(new BigDecimal("50000.50"), savedPrice.getBestAsk());
    }
}
