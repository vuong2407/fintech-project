package com.vuongnguyen.fintech_project.controller;

import com.vuongnguyen.fintech_project.dto.ApiResponse;
import com.vuongnguyen.fintech_project.dto.TradeRequest;
import com.vuongnguyen.fintech_project.dto.TradeResponse;
import com.vuongnguyen.fintech_project.service.TradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
