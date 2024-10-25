package com.abc.tradingstatisticsservice.dto;

import lombok.Data;

@Data
public class TradingDataBatch {
    private String symbol;
    private Float[] values;
}
