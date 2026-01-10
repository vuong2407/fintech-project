package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.HuobiTicker;
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
class HuobiPriceServiceTest {

    @Mock
    private ExternalService externalService;

    @InjectMocks
    private HuobiPriceService huobiPriceService;

    private List<HuobiTicker> mockHuobiResponses;

    @BeforeEach
    void setUp() {
        mockHuobiResponses = Arrays.asList(
                createHuobiTicker("ETHUSDT", "3000.00", "3001.00"),
                createHuobiTicker("BTCUSDT", "50000.00", "50001.00")
        );
    }

    @Test
    void testFetchPrices_Success() {
        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockHuobiResponses);

        List<PriceData> result = huobiPriceService.fetchPrices();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(externalService, times(1)).fetchHuobiPrices(SUPPORTED_SYMBOLS);
    }

    @Test
    void testFetchPrices_VerifyDataMapping() {
        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockHuobiResponses);

        List<PriceData> result = huobiPriceService.fetchPrices();

        PriceData ethPrice = result.stream()
                .filter(p -> p.getSymbol().equals("ETHUSDT"))
                .findFirst()
                .orElseThrow();

        assertEquals("ETHUSDT", ethPrice.getSymbol());
        assertEquals(new BigDecimal("3000.00"), ethPrice.getBid());
        assertEquals(new BigDecimal("3001.00"), ethPrice.getAsk());
        assertEquals(DCESource.HUOBI, ethPrice.getSource());
    }

    @Test
    void testFetchPrices_VerifyAllSymbolsMapped() {
        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockHuobiResponses);

        List<PriceData> result = huobiPriceService.fetchPrices();

        assertTrue(result.stream().allMatch(p -> p.getSource() == DCESource.HUOBI));
        assertEquals(2, result.size());
    }

    @Test
    void testFetchPrices_EmptyResponse() {
        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(Collections.emptyList());

        List<PriceData> result = huobiPriceService.fetchPrices();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(externalService, times(1)).fetchHuobiPrices(SUPPORTED_SYMBOLS);
    }

    @Test
    void testFetchPrices_SingleSymbol() {
        List<HuobiTicker> singleResponse = Collections.singletonList(
                createHuobiTicker("BTCUSDT", "50000.00", "50001.00")
        );

        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(singleResponse);

        List<PriceData> result = huobiPriceService.fetchPrices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BTCUSDT", result.get(0).getSymbol());
        assertEquals(DCESource.HUOBI, result.get(0).getSource());
    }

    @Test
    void testFetchPrices_VerifyBidAskPrices() {
        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(mockHuobiResponses);

        List<PriceData> result = huobiPriceService.fetchPrices();

        for (PriceData priceData : result) {
            assertNotNull(priceData.getBid());
            assertNotNull(priceData.getAsk());
            assertTrue(priceData.getBid().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(priceData.getAsk().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void testFetchPrices_LargePriceValues() {
        List<HuobiTicker> largeValueResponses = List.of(
                createHuobiTicker("BTCUSDT", "99999.99", "100000.01")
        );

        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(largeValueResponses);

        List<PriceData> result = huobiPriceService.fetchPrices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("99999.99"), result.get(0).getBid());
        assertEquals(new BigDecimal("100000.01"), result.get(0).getAsk());
    }

    @Test
    void testFetchPrices_MultipleSymbols() {
        List<HuobiTicker> multipleSymbols = Arrays.asList(
                createHuobiTicker("ETHUSDT", "3000.00", "3001.00"),
                createHuobiTicker("BTCUSDT", "50000.00", "50001.00")
        );

        when(externalService.fetchHuobiPrices(SUPPORTED_SYMBOLS))
                .thenReturn(multipleSymbols);

        List<PriceData> result = huobiPriceService.fetchPrices();

        assertNotNull(result);
        assertEquals(2, result.size());
        
        List<String> symbols = result.stream()
                .map(PriceData::getSymbol)
                .toList();
        
        assertTrue(symbols.contains("ETHUSDT"));
        assertTrue(symbols.contains("BTCUSDT"));
    }

    private HuobiTicker createHuobiTicker(String symbol, String bid, String ask) {
        return new HuobiTicker(symbol, new BigDecimal(bid), new BigDecimal(ask));
    }
}
