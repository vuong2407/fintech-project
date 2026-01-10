package com.vuongnguyen.fintech_project.scheduler;

import com.vuongnguyen.fintech_project.service.PriceAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAggregationScheduler {

    private final PriceAggregationService priceAggregationService;

    @Scheduled(fixedRate = 10000)
    public void aggregatePrices() {
        log.debug("Starting scheduled price aggregation");

        try {
            priceAggregationService.aggregateAndStorePrices();
        } catch (Exception e) {
            log.error("Error during scheduled price aggregation: {}", e.getMessage(), e);
        }
    }
}
