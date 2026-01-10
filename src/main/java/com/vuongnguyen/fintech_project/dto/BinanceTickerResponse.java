package com.vuongnguyen.fintech_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BinanceTickerResponse {

    public BinanceTickerResponse(String symbol, BigDecimal bestBid, BigDecimal bestAsk) {
        this.symbol = symbol;
        this.bidPrice = bestBid;
        this.askPrice = bestAsk;
    }

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("bidPrice")
    private BigDecimal bidPrice;

    @JsonProperty("bidQty")
    private BigDecimal bidQty;

    @JsonProperty("askPrice")
    private BigDecimal askPrice;

    @JsonProperty("askQty")
    private BigDecimal askQty;
}
