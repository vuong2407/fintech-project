package com.vuongnguyen.fintech_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistoryResponse {

    private List<TradeHistoryItem> trades;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
}
