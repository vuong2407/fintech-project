package com.vuongnguyen.fintech_project.dto;

import com.vuongnguyen.fintech_project.entity.Trade;
import com.vuongnguyen.fintech_project.entity.WalletBalance;
import com.vuongnguyen.fintech_project.enums.TradeSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    private Long tradeId;
    private Long userId;
    private String symbol;
    private TradeSide side;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private String clientOrderId;

    private BigDecimal updatedUsdtBalance;
    private BigDecimal updatedAssetBalance;
    private String assetCurrency;

    public TradeResponse toTradeResponse(Trade trade) {
        this.setTradeId(trade.getId());
        this.setUserId(trade.getUser().getId());
        this.setSymbol(trade.getSymbol());
        this.setSide(trade.getSide());
        this.setPrice(trade.getPrice());
        this.setQuantity(trade.getQuantity());
        this.setTotalAmount(trade.getTotalAmount());
        this.setCreatedAt(trade.getCreatedAt());
        this.setClientOrderId(trade.getClientOrderId());

        return this;
    }

    public TradeResponse toTradeResponse(Trade trade, WalletBalance usdtBalance,
                                         WalletBalance assetBalance, String assetCurrency) {
        this.setTradeId(trade.getId());
        this.setUserId(trade.getUser().getId());
        this.setSymbol(trade.getSymbol());
        this.setSide(trade.getSide());
        this.setPrice(trade.getPrice());
        this.setQuantity(trade.getQuantity());
        this.setTotalAmount(trade.getTotalAmount());
        this.setCreatedAt(trade.getCreatedAt());
        this.setClientOrderId(trade.getClientOrderId());
        this.setUpdatedUsdtBalance(usdtBalance.getBalance());
        this.setUpdatedAssetBalance(assetBalance.getBalance());
        this.setAssetCurrency(assetCurrency);

        return this;
    }
}
