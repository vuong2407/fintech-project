package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.BinanceTickerResponse;
import com.vuongnguyen.fintech_project.dto.PriceData;
import com.vuongnguyen.fintech_project.enums.DCESource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.vuongnguyen.fintech_project.utility.Constant.SUPPORTED_SYMBOLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinancePriceServiceTest {

    @Mock
    private ExternalService externalService;

    @InjectMocks
    private BinancePriceService binancePriceService;

    private List<BinanceTickerResponse> mockBinanceResponses;

    @BeforeEach
    void setUp() {
        mockBinanceResponses = Arrays.asList(
                createBinanceResponse("ETHUSDT", "3000.00", "3001.00"),
                createBinanceResponse("BTCUSDT", "50000.00", "50001.00")
        );
    }

    @Test
    void testFetchPrices_Success() {
        when(externalService.fetchBinancePrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockBinanceResponses);

        List<PriceData> result = binancePriceService.fetchPrices();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(externalService, times(1)).fetchBinancePrices(SUPPORTED_SYMBOLS);
    }

    @Test
    void testFetchPrices_VerifyDataMapping() {
        when(externalService.fetchBinancePrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockBinanceResponses);

        List<PriceData> result = binancePriceService.fetchPrices();

        PriceData ethPrice = result.stream()
                .filter(p -> p.getSymbol().equals("ETHUSDT"))
                .findFirst()
                .orElseThrow();

        assertEquals("ETHUSDT", ethPrice.getSymbol());
        assertEquals(new BigDecimal("3000.00"), ethPrice.getBid());
        assertEquals(new BigDecimal("3001.00"), ethPrice.getAsk());
        assertEquals(DCESource.BINANCE, ethPrice.getSource());
    }

    @Test
    void testFetchPrices_VerifyAllSymbolsMapped() {
        when(externalService.fetchBinancePrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockBinanceResponses);

        List<PriceData> result = binancePriceService.fetchPrices();

        assertTrue(result.stream().allMatch(p -> p.getSource() == DCESource.BINANCE));
        assertEquals(2, result.size());
    }

    @Test
    void testFetchPrices_EmptyResponse() {
        when(externalService.fetchBinancePrices(SUPPORTED_SYMBOLS))
                .thenReturn(Collections.emptyList());

        List<PriceData> result = binancePriceService.fetchPrices();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(externalService, times(1)).fetchBinancePrices(SUPPORTED_SYMBOLS);
    }

    @Test
    void testFetchPrices_SingleSymbol() {
        List<BinanceTickerResponse> singleResponse = Collections.singletonList(
                createBinanceResponse("BTCUSDT", "50000.00", "50001.00")
        );

        when(externalService.fetchBinancePrices(SUPPORTED_SYMBOLS))
                .thenReturn(singleResponse);

        List<PriceData> result = binancePriceService.fetchPrices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BTCUSDT", result.get(0).getSymbol());
        assertEquals(DCESource.BINANCE, result.get(0).getSource());
    }

    @Test
    void testFetchPrices_VerifyBidAskPrices() {
        when(externalService.fetchBinancePrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockBinanceResponses);

        List<PriceData> result = binancePriceService.fetchPrices();

        for (PriceData priceData : result) {
            assertNotNull(priceData.getBid());
            assertNotNull(priceData.getAsk());
            assertTrue(priceData.getBid().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(priceData.getAsk().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void testFetchPrices_LargePriceValues() {
        List<BinanceTickerResponse> largeValueResponses = List.of(
                createBinanceResponse("BTCUSDT", "99999.99", "100000.01")
        );

        when(externalService.fetchBinancePrices(SUPPORTED_SYMBOLS))
                .thenReturn(largeValueResponses);

        List<PriceData> result = binancePriceService.fetchPrices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("99999.99"), result.get(0).getBid());
        assertEquals(new BigDecimal("100000.01"), result.get(0).getAsk());
    }

    private BinanceTickerResponse createBinanceResponse(String symbol, String bid, String ask) {
        return new BinanceTickerResponse(symbol, new BigDecimal(bid), new BigDecimal(ask));
    }
}
