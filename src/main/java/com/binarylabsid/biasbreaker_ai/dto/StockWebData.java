package com.binarylabsid.biasbreaker_ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class StockWebData {
    private String symbol;
    private String companyName;
    private String sector;
    private String priceChange;
    private String lastEarnings;
    private String peRatio;
    private String maxDrawdown;
    private String forecastGrowth;

    private String verdictAI;
    private String verdictReason;
    private String bullRead;
    private String bearRead;
    private String beginnerInsightPe;
    private String beginnerInsightRisk;

    private List<Long> chartRevenue;
    private List<Long> chartEarnings;
}
