package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.PriceData;
import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.vuongnguyen.fintech_project.utility.Constant.SUPPORTED_SYMBOLS;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAggregationService {

    private final BinancePriceService binancePriceService;
    private final HuobiPriceService huobiPriceService;
    private final AggregatedPriceRepository aggregatedPriceRepository;

    public void aggregateAndStorePrices() {
        log.info("Starting price aggregation from external exchanges");

        List<PriceData> binancePrices = binancePriceService.fetchPrices();
        List<PriceData> huobiPrices = huobiPriceService.fetchPrices();

        List<PriceData> allPrices = new ArrayList<>();
        allPrices.addAll(binancePrices);
        allPrices.addAll(huobiPrices);

        if (allPrices.isEmpty()) {
            log.warn("No price data received from any exchange");
            return;
        }

        Map<String, List<PriceData>> pricesBySymbol = allPrices.stream()
                .collect(Collectors.groupingBy(PriceData::getSymbol));

        for (String symbol : SUPPORTED_SYMBOLS) {
            List<PriceData> symbolPrices = pricesBySymbol.get(symbol);

            if (CollectionUtils.isEmpty(symbolPrices)) {
                log.warn("No price data available for symbol: {}", symbol);
                continue;
            }

            try {
                AggregatedPrice aggregatedPrice = calculateBestPrices(symbol, symbolPrices);
                aggregatedPriceRepository.save(aggregatedPrice);

                log.info("Saved aggregated price for {}: bestBid={}, bestAsk={}, sources={}",
                        symbol, aggregatedPrice.getBestBid(), aggregatedPrice.getBestAsk(),
                        symbolPrices.stream().map(PriceData::getSource).collect(Collectors.toSet()));

            } catch (Exception e) {
                log.error("Error aggregating prices for symbol {}: {}", symbol, e.getMessage(), e);
            }
        }

        log.info("Price aggregation completed");
    }

    private AggregatedPrice calculateBestPrices(String symbol, List<PriceData> prices) {
        BigDecimal bestBid = prices.stream()
                .map(PriceData::getBid)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalStateException("No valid bid prices found for " + symbol));

        BigDecimal bestAsk = prices.stream()
                .map(PriceData::getAsk)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalStateException("No valid ask prices found for " + symbol));

        if (bestBid.compareTo(bestAsk) > 0) {
            log.warn("Best bid ({}) is higher than best ask ({}) for {}. This might indicate data issues.",
                    bestBid, bestAsk, symbol);
        }

        AggregatedPrice aggregatedPrice = new AggregatedPrice();
        aggregatedPrice.setSymbol(symbol);
        aggregatedPrice.setBestBid(bestBid);
        aggregatedPrice.setBestAsk(bestAsk);
        aggregatedPrice.setTimestamp(LocalDateTime.now());

        return aggregatedPrice;
    }

    public Optional<AggregatedPrice> getLatestPrice(String symbol) {
        return aggregatedPriceRepository.findLatestBySymbol(symbol);
    }
}
