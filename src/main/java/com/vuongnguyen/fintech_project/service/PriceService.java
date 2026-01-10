package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.AggregatedPriceResponse;
import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    private final AggregatedPriceRepository aggregatedPriceRepository;

    public Optional<AggregatedPriceResponse> getLatestPrice(String symbol) {
        log.debug("Fetching latest price for symbol: {}", symbol);

        Optional<AggregatedPrice> latestPrice = aggregatedPriceRepository.findLatestBySymbol(symbol);

        if (latestPrice.isPresent()) {
            AggregatedPrice price = latestPrice.get();
            AggregatedPriceResponse response = new AggregatedPriceResponse(price.getSymbol(), price.getBestBid(),
                    price.getBestAsk(), price.getTimestamp());

            log.debug("Found latest price for {}: bid={}, ask={}, timestamp={}",
                    symbol, price.getBestBid(), price.getBestAsk(), price.getTimestamp());

            return Optional.of(response);
        }

        log.warn("No price data found for symbol: {}", symbol);
        return Optional.empty();
    }
}
