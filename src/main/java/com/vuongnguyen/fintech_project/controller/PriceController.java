package com.vuongnguyen.fintech_project.controller;

import com.vuongnguyen.fintech_project.dto.AggregatedPriceResponse;
import com.vuongnguyen.fintech_project.dto.ApiResponse;
import com.vuongnguyen.fintech_project.service.PriceService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
@Slf4j
public class PriceController {

    private final PriceService priceService;

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<AggregatedPriceResponse>> getLatestPrice(
            @RequestParam @NotBlank(message = "Symbol is required") String symbol) {

        log.info("Received request for latest price of symbol: {}", symbol);

        String normalizedSymbol = symbol.toUpperCase().trim();

        Optional<AggregatedPriceResponse> latestPrice = priceService.getLatestPrice(normalizedSymbol);

        return latestPrice.map(aggregatedPriceResponse -> ResponseEntity.ok(ApiResponse.success("Latest price retrieved successfully", aggregatedPriceResponse)))
                .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }
}
