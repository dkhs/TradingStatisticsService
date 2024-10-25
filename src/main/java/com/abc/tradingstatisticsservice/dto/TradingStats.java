package com.abc.tradingstatisticsservice.dto;

import lombok.Data;

@Data
public class TradingStats {
    private double min;
    private double max;
    private double last;
    private double avg;
    private double variance;
}
