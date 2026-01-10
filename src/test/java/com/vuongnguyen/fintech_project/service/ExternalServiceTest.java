package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.BinanceTickerResponse;
import com.vuongnguyen.fintech_project.dto.HuobiTicker;
import com.vuongnguyen.fintech_project.dto.HuobiTickerResponse;
import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExternalServiceTest {

    @Autowired
    private ExternalService externalService;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private List<String> testSymbols;
    private BinanceTickerResponse[] binanceResponses;
    private HuobiTickerResponse huobiResponse;
    private List<AggregatedPrice> fallbackPrices;

    @BeforeEach
    void setUp() {
        testSymbols = Arrays.asList("BTCUSDT", "ETHUSDT");

        binanceResponses = new BinanceTickerResponse[]{
                createBinanceResponse("BTCUSDT", "50000.00", "50001.00"),
                createBinanceResponse("ETHUSDT", "3000.00", "3001.00")
        };

        huobiResponse = new HuobiTickerResponse();
        huobiResponse.setStatus("ok");
        huobiResponse.setData(Arrays.asList(
                createHuobiTicker("btcusdt", "50000.00", "50001.00"),
                createHuobiTicker("ethusdt", "3000.00", "3001.00")
        ));

        fallbackPrices = Arrays.asList(
                createAggregatedPrice(1L, "BTCUSDT", "49000.00", "49001.00"),
                createAggregatedPrice(2L, "ETHUSDT", "2900.00", "2901.00")
        );

        resetCircuitBreakers();
        reset(restTemplate, aggregatedPriceRepository);
    }

    private void resetCircuitBreakers() {
        CircuitBreaker binanceCB = circuitBreakerRegistry.circuitBreaker("fetchingBinancePricesCircuitBreaker");
        CircuitBreaker huobiCB = circuitBreakerRegistry.circuitBreaker("fetchingHoubiPricesCircuitBreaker");
        binanceCB.reset();
        huobiCB.reset();
    }

    @Test
    void testBinance_SuccessfulCall() {
        when(restTemplate.getForObject(anyString(), eq(BinanceTickerResponse[].class)))
                .thenReturn(binanceResponses);

        List<BinanceTickerResponse> result = externalService.fetchBinancePrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BTCUSDT", result.get(0).getSymbol());
        assertEquals(new BigDecimal("50000.00"), result.get(0).getBidPrice());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(BinanceTickerResponse[].class));
    }


    @Test
    void testBinance_FallbackAfterAllRetriesFail() {
        when(restTemplate.getForObject(anyString(), eq(BinanceTickerResponse[].class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        List<BinanceTickerResponse> result = externalService.fetchBinancePrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BTCUSDT", result.get(0).getSymbol());
        assertEquals(new BigDecimal("49000.00"), result.get(0).getBidPrice());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbolIn(testSymbols);
    }

    @Test
    void testBinance_CircuitBreakerOpens() {
        when(restTemplate.getForObject(anyString(), eq(BinanceTickerResponse[].class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        for (int i = 0; i < 6; i++) {
            externalService.fetchBinancePrices(testSymbols);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchingBinancePricesCircuitBreaker");
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    void testBinance_CircuitBreakerFallback() {
        when(restTemplate.getForObject(anyString(), eq(BinanceTickerResponse[].class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        for (int i = 0; i < 6; i++) {
            externalService.fetchBinancePrices(testSymbols);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchingBinancePricesCircuitBreaker");
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        List<BinanceTickerResponse> result = externalService.fetchBinancePrices(testSymbols);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BTCUSDT", result.get(0).getSymbol());
    }

    @Test
    void testHuobi_SuccessfulCall() {
        when(restTemplate.getForObject(anyString(), eq(HuobiTickerResponse.class)))
                .thenReturn(huobiResponse);

        List<HuobiTicker> result = externalService.fetchHuobiPrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("btcusdt", result.get(0).getSymbol());
        assertEquals(new BigDecimal("50000.00"), result.get(0).getBid());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(HuobiTickerResponse.class));
    }


    @Test
    void testHuobi_FallbackAfterAllRetriesFail() {
        when(restTemplate.getForObject(anyString(), eq(HuobiTickerResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        List<HuobiTicker> result = externalService.fetchHuobiPrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BTCUSDT", result.get(0).getSymbol());
        assertEquals(new BigDecimal("49000.00"), result.get(0).getBid());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbolIn(testSymbols);
    }

    @Test
    void testHuobi_CircuitBreakerOpens() {
        when(restTemplate.getForObject(anyString(), eq(HuobiTickerResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        for (int i = 0; i < 6; i++) {
            externalService.fetchHuobiPrices(testSymbols);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchingHoubiPricesCircuitBreaker");
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    void testBinance_SymbolFiltering() {
        BinanceTickerResponse[] allResponses = new BinanceTickerResponse[]{
                createBinanceResponse("BTCUSDT", "50000.00", "50001.00"),
                createBinanceResponse("ETHUSDT", "3000.00", "3001.00"),
                createBinanceResponse("BNBUSDT", "400.00", "401.00")
        };

        when(restTemplate.getForObject(anyString(), eq(BinanceTickerResponse[].class)))
                .thenReturn(allResponses);

        List<BinanceTickerResponse> result = externalService.fetchBinancePrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> testSymbols.contains(r.getSymbol())));
    }

    @Test
    void testHuobi_SymbolFiltering() {
        HuobiTickerResponse allResponse = new HuobiTickerResponse();
        allResponse.setStatus("ok");
        allResponse.setData(Arrays.asList(
                createHuobiTicker("BTCUSDT", "50000.00", "50001.00"),
                createHuobiTicker("ETHUSDT", "3000.00", "3001.00"),
                createHuobiTicker("BNBUSDT", "400.00", "401.00")
        ));

        when(restTemplate.getForObject(anyString(), eq(HuobiTickerResponse.class)))
                .thenReturn(allResponse);

        List<HuobiTicker> result = externalService.fetchHuobiPrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> testSymbols.contains(r.getSymbol())));
    }

    @Test
    void testBinance_NullResponseHandling() {
        when(restTemplate.getForObject(anyString(), eq(BinanceTickerResponse[].class)))
                .thenReturn(null);

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        List<BinanceTickerResponse> result = externalService.fetchBinancePrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbolIn(testSymbols);
    }

    @Test
    void testHuobi_NullResponseHandling() {
        when(restTemplate.getForObject(anyString(), eq(HuobiTickerResponse.class)))
                .thenReturn(null);

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        List<HuobiTicker> result = externalService.fetchHuobiPrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbolIn(testSymbols);
    }

    @Test
    void testHuobi_NullDataHandling() {
        HuobiTickerResponse nullDataResponse = new HuobiTickerResponse();
        nullDataResponse.setStatus("ok");
        nullDataResponse.setData(null);

        when(restTemplate.getForObject(anyString(), eq(HuobiTickerResponse.class)))
                .thenReturn(nullDataResponse);

        when(aggregatedPriceRepository.findLatestBySymbolIn(testSymbols))
                .thenReturn(fallbackPrices);

        List<HuobiTicker> result = externalService.fetchHuobiPrices(testSymbols);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(aggregatedPriceRepository, times(1)).findLatestBySymbolIn(testSymbols);
    }

    private BinanceTickerResponse createBinanceResponse(String symbol, String bid, String ask) {
        return new BinanceTickerResponse(symbol, new BigDecimal(bid), new BigDecimal(ask));
    }

    private HuobiTicker createHuobiTicker(String symbol, String bid, String ask) {
        return new HuobiTicker(symbol, new BigDecimal(bid), new BigDecimal(ask));
    }

    private AggregatedPrice createAggregatedPrice(Long id, String symbol, String bid, String ask) {
        return new AggregatedPrice(id, symbol, new BigDecimal(bid), new BigDecimal(ask), LocalDateTime.now());
    }
}
