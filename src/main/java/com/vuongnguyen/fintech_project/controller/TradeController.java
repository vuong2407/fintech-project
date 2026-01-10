package com.vuongnguyen.fintech_project.controller;

import com.vuongnguyen.fintech_project.dto.ApiResponse;
import com.vuongnguyen.fintech_project.dto.TradeHistoryResponse;
import com.vuongnguyen.fintech_project.dto.TradeRequest;
import com.vuongnguyen.fintech_project.dto.TradeResponse;
import com.vuongnguyen.fintech_project.service.TradingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
@Slf4j
public class TradeController {

    private final TradingService tradingService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradeResponse>> executeTrade(@Valid @RequestBody TradeRequest request) {
        log.info("Received trade request: userId={}, symbol={}, side={}, quantity={}",
                request.getUserId(), request.getSymbol(), request.getSide(), request.getQuantity());

        TradeResponse tradeResponse = tradingService.executeUserTrading(request);

        log.info("Trade executed successfully: tradeId={}, userId={}, symbol={}",
                tradeResponse.getTradeId(), tradeResponse.getUserId(), tradeResponse.getSymbol());

        return ResponseEntity.ok(ApiResponse.success("Trade executed successfully", tradeResponse));
    }

    @GetMapping("/history/user/{userId}")
    public ResponseEntity<ApiResponse<TradeHistoryResponse>> getUserTradeHistory(
            @PathVariable @NotNull @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String symbol) {

        log.info("Received request for trade history: userId={}, page={}, size={}, symbol={}",
                userId, page, size, symbol);

        if (symbol != null && !symbol.trim().isEmpty()) {
            symbol = symbol.toUpperCase().trim();
        }

        TradeHistoryResponse tradeHistory = tradingService.getUserTradeHistory(userId, page, size, symbol);

        if (tradeHistory.getTrades().isEmpty() && page == 0) {
            log.info("No trade history found for user: {}", userId);
            return ResponseEntity.ok(ApiResponse.success("No trade history found", tradeHistory));
        }

        if (tradeHistory.getTrades().isEmpty()) {
            log.warn("Page {} not found for user: {} trade history", page, userId);
            return ResponseEntity.badRequest().body(ApiResponse.error("Page not found. Total pages: " + tradeHistory.getTotalPages()));
        }

        log.info("Successfully retrieved trade history for user: {}, page: {}, total trades: {}",
                userId, page, tradeHistory.getTrades().size());

        return ResponseEntity.ok(ApiResponse.success("Trade history retrieved successfully", tradeHistory));
    }
}
