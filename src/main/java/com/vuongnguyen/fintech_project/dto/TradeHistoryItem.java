package com.vuongnguyen.fintech_project.dto;

import com.vuongnguyen.fintech_project.entity.Trade;
import com.vuongnguyen.fintech_project.enums.TradeSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistoryItem {

    private Long tradeId;
    private String symbol;
    private TradeSide side;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private String clientOrderId;

    public TradeHistoryItem toTradeHistoryItem(Trade trade) {
        this.setTradeId(trade.getId());
        this.setSymbol(trade.getSymbol());
        this.setSide(trade.getSide());
        this.setPrice(trade.getPrice());
        this.setQuantity(trade.getQuantity());
        this.setTotalAmount(trade.getTotalAmount());
        this.setCreatedAt(trade.getCreatedAt());
        this.setClientOrderId(trade.getClientOrderId());

        return this;
    }
}
