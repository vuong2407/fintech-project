package com.vuongnguyen.fintech_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class HuobiTicker {

    public HuobiTicker(String symbol, BigDecimal bestBid, BigDecimal bestAsk) {
        this.symbol = symbol;
        this.bid = bestBid;
        this.ask = bestAsk;
    }

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("bid")
    private BigDecimal bid;

    @JsonProperty("ask")
    private BigDecimal ask;

    @JsonProperty("open")
    private BigDecimal open;

    @JsonProperty("high")
    private BigDecimal high;

    @JsonProperty("low")
    private BigDecimal low;

    @JsonProperty("close")
    private BigDecimal close;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("vol")
    private BigDecimal vol;

    @JsonProperty("count")
    private Integer count;
}
