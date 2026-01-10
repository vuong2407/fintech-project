package com.vuongnguyen.fintech_project.dto;

import com.vuongnguyen.fintech_project.enums.DCESource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceData {

    private String symbol;
    private BigDecimal bid;
    private BigDecimal ask;
    private DCESource source;
}
