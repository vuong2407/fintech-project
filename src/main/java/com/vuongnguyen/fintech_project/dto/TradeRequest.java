package com.vuongnguyen.fintech_project.dto;

import com.vuongnguyen.fintech_project.enums.TradeSide;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Trade side is required")
    private TradeSide side;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0.00000001")
    @DecimalMax(value = "1000000", message = "Quantity cannot exceed 1,000,000")
    @Digits(integer = 10, fraction = 8, message = "Quantity can have maximum 10 integer digits and 8 decimal places")
    private BigDecimal quantity;

    @Size(max = 50, message = "Client order ID cannot exceed 50 characters")
    private String clientOrderId;
}
