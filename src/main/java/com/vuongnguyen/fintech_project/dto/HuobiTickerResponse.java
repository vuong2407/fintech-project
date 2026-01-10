package com.vuongnguyen.fintech_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class HuobiTickerResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private List<HuobiTicker> data;
}
