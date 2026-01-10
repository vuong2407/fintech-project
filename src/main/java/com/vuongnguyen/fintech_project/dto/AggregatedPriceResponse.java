package com.vuongnguyen.fintech_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedPriceResponse {

    private String symbol;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private LocalDateTime timestamp;
}
