package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.BinanceTickerResponse;
import com.vuongnguyen.fintech_project.dto.HuobiTicker;
import com.vuongnguyen.fintech_project.dto.HuobiTickerResponse;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalService {

    private final RestTemplate restTemplate;

    private final AggregatedPriceRepository aggregatedPriceRepository;

    @Value("${dce.binance.url}")
    private String binanceUrl;

    @Value("${dce.huobi.url}")
    private String huobiUrl;

    /**
     * Fetches current price data from Binance API for the specified symbols.
     * This method is protected by circuit breaker and retry patterns for resilience.
     *
     * @param symbols List of trading symbols to fetch prices
     * @return List of BinanceTickerResponse containing bid/ask prices for requested symbols
     * @throws IllegalStateException if the Binance API response is null
     */
    @Retry(name = "fetchingBinancePricesRetry")
    @CircuitBreaker(
            name = "fetchingBinancePricesCircuitBreaker",
            fallbackMethod = "fetchBinancePricesFallback"
    )
    public List<BinanceTickerResponse> fetchBinancePrices(List<String> symbols) {

        BinanceTickerResponse[] response =
                restTemplate.getForObject(binanceUrl, BinanceTickerResponse[].class);

        if (Objects.isNull(response)) {
            throw new IllegalStateException("Binance response is null");
        }

        return Arrays.stream(response)
                .filter(res -> symbols.contains(res.getSymbol()))
                .toList();
    }

    /**
     * Fallback method for fetchBinancePrices when circuit breaker is open or retries are exhausted.
     * Returns the latest aggregated price data from the database as a fallback mechanism.
     *
     * @param ex The exception that triggered the fallback
     * @param symbols List of trading symbols to fetch fallback prices for
     * @return List of BinanceTickerResponse constructed from cached aggregated price data
     */
    private List<BinanceTickerResponse> fetchBinancePricesFallback(List<String> symbols, Throwable ex) {
        log.error("Binance fetch failed, fallback triggered: {}", ex.getMessage());

        return aggregatedPriceRepository.findLatestBySymbolIn(symbols).stream()
                .map(ap -> new BinanceTickerResponse(ap.getSymbol(), ap.getBestBid(), ap.getBestAsk()))
                .toList();
    }

    /**
     * Fetches current price data from Huobi API for the specified symbols.
     * This method is protected by circuit breaker and retry patterns for resilience.
     * Filters response data to only include symbols that match the requested list.
     *
     * @param symbols List of trading symbols to fetch prices
     * @return List of HuobiTicker containing bid/ask prices for requested symbols
     * @throws IllegalStateException if the Huobi API response or data is null
     */
    @Retry(name = "fetchingHoubiPricesRetry")
    @CircuitBreaker(
            name = "fetchingHoubiPricesCircuitBreaker",
            fallbackMethod = "fetchHoubiPricesFallback"
    )
    public List<HuobiTicker> fetchHuobiPrices(List<String> symbols) {
        HuobiTickerResponse response = restTemplate.getForObject(huobiUrl, HuobiTickerResponse.class);

        if (Objects.isNull(response) || Objects.isNull(response.getData())) {
            throw new IllegalStateException("Houbi response is null");
        }

        return response.getData().stream()
                .filter(res -> symbols.contains(res.getSymbol().toUpperCase()))
                .toList();
    }

    /**
     * Fallback method for fetchHuobiPrices when circuit breaker is open or retries are exhausted.
     * Returns the latest aggregated price data from the database as a fallback mechanism.
     *
     * @param ex The exception that triggered the fallback
     * @param symbols List of trading symbols to fetch fallback prices for
     * @return List of HuobiTicker constructed from cached aggregated price data
     */
    private List<HuobiTicker> fetchHoubiPricesFallback(List<String> symbols, Throwable ex) {
        log.error("Houbi fetch failed, fallback triggered: {}", ex.getMessage());

        return aggregatedPriceRepository.findLatestBySymbolIn(symbols).stream()
                .map(ap -> new HuobiTicker(ap.getSymbol(), ap.getBestBid(), ap.getBestAsk()))
                .toList();
    }
}
