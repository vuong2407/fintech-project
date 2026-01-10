package com.vuongnguyen.fintech_project.dto;

import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.enums.TradeSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TradeDetails {

    private BigDecimal price;
    private BigDecimal totalAmount;

    public TradeDetails toTradeDetails(TradeSide tradeSide, BigDecimal quantity, AggregatedPrice latestPrice) {
        BigDecimal price;
        BigDecimal totalAmount;

        if (TradeSide.BUY.equals(tradeSide)) {
            price = latestPrice.getBestAsk();
        } else {
            price = latestPrice.getBestBid();
        }
        totalAmount = price.multiply(quantity).setScale(8, RoundingMode.HALF_UP);

        this.price = price;
        this.totalAmount = totalAmount;

        return this;
    }
}
